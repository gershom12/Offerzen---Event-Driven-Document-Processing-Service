package com.document.application.service;

import com.document.application.processor.DocumentEventProcessor;
import com.document.domain.model.DocumentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestionService {

    private final DocumentEventProcessor processor;

    public void ingest(DocumentEvent event) {
        processor.process(event);
    }
}