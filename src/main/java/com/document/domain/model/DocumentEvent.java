package com.document.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class DocumentEvent {
    private final String eventId;
    private final String documentId;
    private final long sequence;
    private final Instant timestamp;
    private final EventType type;
    private final Map<String, Object> payload;
}