package com.document.domain.policy;

import org.springframework.stereotype.Component;

@Component
public class SequencePolicy {

    public boolean canApply(long lastSeq, long incomingSeq) {
        return incomingSeq == lastSeq + 1;
    }
}