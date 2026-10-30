package com.wikimind.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

// One chunk of a document, stored alongside its embedding so it can be retrieved by vector similarity search.
@Entity
@Table(name = "wiki_documents")
public class WikiDocument {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "source_title")
    private String sourceTitle;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(columnDefinition = "TEXT")
    private String text;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    public WikiDocument() {
    }

    public WikiDocument(String sourceTitle, String orgId, String text, float[] embedding) {
        this.sourceTitle = sourceTitle;
        this.orgId = orgId;
        this.text = text;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
