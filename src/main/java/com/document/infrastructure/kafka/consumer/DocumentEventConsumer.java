package com.document.infrastructure.kafka.consumer;

import com.document.application.processor.DocumentEventProcessor;
import com.document.domain.model.DocumentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventConsumer {

    private final DocumentEventProcessor processor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "document-events", groupId = "doc-service")
    public void consume(String message) {

        try {
            DocumentEvent event =
                    objectMapper.readValue(message, DocumentEvent.class);

            processor.process(event);

        } catch (Exception ex) {
            log.error("CONSUMER_FAILURE message={}", message, ex);
            // retry mechanism triggers
            throw new RuntimeException(ex);
        }
    }
}