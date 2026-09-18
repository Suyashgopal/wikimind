package com.wikimind.repository;

import com.wikimind.model.WikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WikiDocumentRepository extends JpaRepository<WikiDocument, String> {

    /**
     * Nearest-neighbour search using PGVector's cosine distance operator ({@code <=>}).
     * Rows are ordered by distance to the query vector, closest first.
     */
    @Query(value = """
        SELECT id, source_title, text, embedding
        FROM wiki_documents
        ORDER BY embedding <=> cast(:queryVector AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<WikiDocument> searchSimilar(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );

    boolean existsBySourceTitle(String sourceTitle);

    long countBySourceTitle(String sourceTitle);
}
