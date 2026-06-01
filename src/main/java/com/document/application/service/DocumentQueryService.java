package com.document.application.service;

import com.document.domain.model.Document;
import com.document.infrastructure.persistence.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentQueryService {

    private final DocumentRepository repository;

    public Optional<Document> getDocument(String id) {
        return repository.findById(id)
                .map(e -> Document.builder()
                        .documentId(e.getDocumentId())
                        .content(e.getContent())
                        .lastSequence(e.getLastSequence())
                        .updatedAt(e.getUpdatedAt())
                        .build());
    }
}