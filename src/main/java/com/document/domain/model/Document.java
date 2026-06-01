package com.document.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
public class Document {
    private final String documentId;
    private final String content;
    private final long lastSequence;
    private final Instant updatedAt;
}