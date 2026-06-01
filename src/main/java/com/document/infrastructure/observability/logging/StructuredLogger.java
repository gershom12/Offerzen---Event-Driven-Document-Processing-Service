package com.document.infrastructure.observability.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class StructuredLogger {

    public void info(String message, Map<String, Object> fields) {
        log.info("{} | {}", message, fields);
    }

    public void warn(String message, Map<String, Object> fields) {
        log.warn("{} | {}", message, fields);
    }

    public void error(String message, Map<String, Object> fields, Throwable t) {
        log.error("{} | {}", message, fields, t);
    }
}