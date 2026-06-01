package com.document.application.processor;

import com.document.domain.model.DocumentEvent;
import com.document.domain.policy.SequencePolicy;
import com.document.infrastructure.persistence.entity.DocumentEntity;
import com.document.infrastructure.persistence.entity.ProcessedEventEntity;
import com.document.infrastructure.persistence.repository.DocumentRepository;
import com.document.infrastructure.persistence.repository.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEventProcessor {

    private final DocumentRepository documentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final SequencePolicy sequencePolicy;

    @Transactional
    public void process(DocumentEvent event) {

        log.info("PROCESS_START eventId={} docId={} seq={}",
                event.getEventId(),
                event.getDocumentId(),
                event.getSequence());

        // 1. IDEMPOTENCY
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("DUPLICATE event ignored {}", event.getEventId());
            return;
        }

        DocumentEntity doc = documentRepository.findById(event.getDocumentId())
                .orElseGet(() -> create(event));

        // 2. ORDERING GUARANTEE
        if (!sequencePolicy.canApply(doc.getLastSequence(), event.getSequence())) {
            log.warn("OUT_OF_ORDER eventId={} expectedSeq={} actualSeq={}",
                    event.getEventId(),
                    doc.getLastSequence() + 1,
                    event.getSequence());
            return;
        }

        try {
            apply(doc, event);

            documentRepository.save(doc);

            processedEventRepository.save(
                    new ProcessedEventEntity(
                            event.getEventId(),
                            event.getDocumentId(),
                            Instant.now()
                    )
            );

            log.info("PROCESS_SUCCESS eventId={}", event.getEventId());

        } catch (Exception ex) {
            log.error("PROCESS_FAILED eventId={}", event.getEventId(), ex);
            // triggers retry
            throw new RuntimeException(ex);
        }
    }

    private void apply(DocumentEntity doc, DocumentEvent event) {
        doc.setContent(event.getPayload().toString());
        doc.setLastSequence(event.getSequence());
        doc.setUpdatedAt(Instant.now());
    }

    private DocumentEntity create(DocumentEvent event) {
        DocumentEntity doc = new DocumentEntity();
        doc.setDocumentId(event.getDocumentId());
        doc.setContent("{}");
        doc.setLastSequence(0);
        doc.setUpdatedAt(Instant.now());
        return doc;
    }
}