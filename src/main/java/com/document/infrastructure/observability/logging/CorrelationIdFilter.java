package com.document.infrastructure.observability.logging;

import jakarta.servlet.*;
import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String id = UUID.randomUUID().toString();

        try {
            MDC.put("correlationId", id);
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}