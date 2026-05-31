package com.ecodatahub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2_000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = getRequestId(request);
        long startedAt = System.nanoTime();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.info(
                    "request.start method={} path={} query={} remote={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    request.getRemoteAddr()
            );

            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();

            if (status >= 500 || durationMs >= SLOW_REQUEST_THRESHOLD_MS) {
                log.warn(
                        "request.end method={} path={} status={} durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        durationMs
                );
            } else {
                log.info(
                        "request.end method={} path={} status={} durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        durationMs
                );
            }

            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }

        return requestId;
    }
}
