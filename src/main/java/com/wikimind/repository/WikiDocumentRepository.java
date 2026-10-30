package com.wikimind.repository;

import com.wikimind.model.WikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WikiDocumentRepository extends JpaRepository<WikiDocument, String> {

    //nearest serch using pgvec cosine distance operator restricted to the caller's own organization rows are ordered by distance to the query vector, closest first.

    @Query(value = """
        SELECT id, source_title, org_id, text, embedding
        FROM wiki_documents
        WHERE org_id = :orgId
        ORDER BY embedding <=> cast(:queryVector AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<WikiDocument> searchSimilar(
            @Param("queryVector") String queryVector,
            @Param("orgId") String orgId,
            @Param("topK") int topK
    );
}
