package com.wikimind.service;

import com.wikimind.model.WikiDocument;
import com.wikimind.repository.WikiDocumentRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Orchestrates the RAG pipeline: chunk + embed + store on ingest,
 * then embed + retrieve + prompt-stuff + generate on query.
 */
@Service
public class RagService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int TOP_K = 3;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private WikiDocumentRepository wikiDocumentRepository;

    /**
     * Splits the content into overlapping chunks, embeds each one,
     * and stores it under the given source title.
     */
    public int ingest(String sourceTitle, String content) {
        List<String> chunks = chunkText(content);
        for (String chunk : chunks) {
            float[] vector = embeddingModel.embed(chunk);
            wikiDocumentRepository.save(new WikiDocument(sourceTitle, chunk, vector));
        }
        return chunks.size();
    }

    /**
     * Embeds the question, retrieves the most relevant stored chunks,
     * and asks the chat model to answer using only that context.
     */
    public String answer(String question) {
        float[] queryVector = embeddingModel.embed(question);
        String vectorLiteral = toVectorLiteral(queryVector);

        List<WikiDocument> topDocs = wikiDocumentRepository.searchSimilar(vectorLiteral, TOP_K);
        if (topDocs.isEmpty()) {
            return "I don't have any documents to answer that yet — ingest something first.";
        }

        String context = topDocs.stream()
                .map(WikiDocument::getText)
                .collect(Collectors.joining("\n---\n"));

        String prompt = """
                Answer the question using ONLY the context below. \
                If the context doesn't contain the answer, say you don't know.

                Context:
                %s

                Question: %s
                Answer:""".formatted(context, question);

        return chatModel.call(prompt);
    }

    private List<String> chunkText(String text) {
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }
        int step = CHUNK_SIZE - CHUNK_OVERLAP;
        return IntStream.iterate(0, start -> start < normalized.length(), start -> start + step)
                .mapToObj(start -> normalized.substring(start, Math.min(start + CHUNK_SIZE, normalized.length())))
                .toList();
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        return sb.append("]").toString();
    }
}
