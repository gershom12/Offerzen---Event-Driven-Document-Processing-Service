package com.document.infrastructure.kafka.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DLQPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String message, String reason) {

        log.warn("DLQ_SEND reason={}", reason);

        kafkaTemplate.send("document-events-dlq", message);
    }
}