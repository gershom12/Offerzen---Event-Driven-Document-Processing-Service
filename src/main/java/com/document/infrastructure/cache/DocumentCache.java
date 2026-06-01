package com.document.infrastructure.cache;

import com.document.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class DocumentCache {

    private final Map<String, DocumentEntity> cache = new ConcurrentHashMap<>();

    public Optional<DocumentEntity> get(String documentId) {
        return Optional.ofNullable(cache.get(documentId));
    }

    public void put(DocumentEntity entity) {
        cache.put(entity.getDocumentId(), entity);
    }

    public void evict(String documentId) {
        cache.remove(documentId);
    }

    public void clear() {
        cache.clear();
    }
}