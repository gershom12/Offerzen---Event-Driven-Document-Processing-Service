package com.document.infrastructure.kafka.config;

import com.document.infrastructure.kafka.dlq.DLQPublisher;
import com.sun.org.apache.xerces.internal.util.DefaultErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class RetryConfig {

    @Bean
    public DefaultErrorHandler errorHandler(DLQPublisher dlqPublisher) {

        FixedBackOff backOff = new FixedBackOff(1000L, 3);

        return new DefaultErrorHandler(
        );
    }
}