package com.company.finance_api.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID = "correlationId";
    private static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId =
                request.getHeader(HEADER_NAME) != null
                        ? request.getHeader(HEADER_NAME)
                        : UUID.randomUUID().toString();

        MDC.put(CORRELATION_ID, correlationId);
        MDC.put("traceId", correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        response.setHeader("X-Trace-Id", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // thread temizliği
        }
    }
}
