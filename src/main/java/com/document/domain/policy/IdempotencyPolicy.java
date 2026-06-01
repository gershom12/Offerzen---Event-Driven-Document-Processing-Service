package com.document.domain.policy;

import org.springframework.stereotype.Component;

@Component
public class IdempotencyPolicy {

    public boolean isDuplicate(boolean exists) {
        return exists;
    }
}