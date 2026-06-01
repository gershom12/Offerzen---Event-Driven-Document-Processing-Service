package com.document.application.service;

import com.document.infrastructure.cache.DocumentCache;
import com.document.infrastructure.persistence.entity.DocumentEntity;
import com.document.infrastructure.persistence.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStateService {

    private final DocumentRepository documentRepository;
    private final DocumentCache documentCache;

    /**
     * Load document from cache first, then DB.
     * This reduces DB load significantly for hot documents.
     */
    public DocumentEntity load(String documentId) {

        // 1. Check cache (HOT PATH)
        Optional<DocumentEntity> cached = documentCache.get(documentId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Fallback to DB
        return documentRepository.findById(documentId)
                .map(entity -> {
                    documentCache.put(entity);
                    return entity;
                })
                .orElse(null);
    }

    /**
     * Create a new document if it does not exist.
     */
    public DocumentEntity create(String documentId) {

        DocumentEntity entity = new DocumentEntity();
        entity.setDocumentId(documentId);
        entity.setContent("{}");
        entity.setLastSequence(0);
        entity.setUpdatedAt(Instant.now());

        DocumentEntity saved = documentRepository.save(entity);
        documentCache.put(saved);

        log.info("DOCUMENT_CREATED id={}", documentId);

        return saved;
    }

    /**
     * Apply update to document state.
     * MUST be called only after:
     * - idempotency check
     * - sequence validation
     */
    @Transactional
    public DocumentEntity applyUpdate(DocumentEntity doc, String newContent, long sequence) {

        doc.setContent(newContent);
        doc.setLastSequence(sequence);
        doc.setUpdatedAt(Instant.now());

        DocumentEntity saved = documentRepository.save(doc);

        // keep cache in sync
        documentCache.put(saved);

        log.debug("DOCUMENT_UPDATED id={} seq={}", doc.getDocumentId(), sequence);

        return saved;
    }

    /**
     * Convenience helper for processor layer.
     */
    public DocumentEntity getOrCreate(String documentId) {

        DocumentEntity existing = load(documentId);
        if (existing != null) {
            return existing;
        }

        return create(documentId);
    }
}