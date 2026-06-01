package com.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(name = "idx_documents_document_id", columnList = "documentId")
        }
)
@Getter
@Setter
public class DocumentEntity {

    /**
     * Business identifier.
     * Acts as partition key in Kafka + primary DB lookup key.
     */
    @Id
    @Column(nullable = false, updatable = false, length = 100)
    private String documentId;

    /**
     * Full document content (JSON/text).
     * Stored as TEXT for flexibility (legal docs may be large).
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Last successfully applied sequence number.
     * Used for ordering guarantee per document.
     */
    @Column(nullable = false)
    private long lastSequence;

    /**
     * Last modification timestamp.
     */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Creation timestamp - IMPORTANT for audit/legal systems.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic locking version.
     *
     * Prevents lost updates when multiple consumers/processors
     * attempt concurrent writes to the same document.
     */
    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}