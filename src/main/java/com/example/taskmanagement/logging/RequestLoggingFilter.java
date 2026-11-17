package com.example.taskmanagement.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    public static final String REQUEST_ID = "requestId";

    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.nanoTime();

        // Always generate a trusted server-side request ID.
        String requestId = UUID.randomUUID().toString();

        MDC.put(REQUEST_ID, requestId);

        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);

        } catch (ServletException | IOException | RuntimeException ex) {
            
            
            if (!response.isCommitted()
                    && response.getStatus() < 400)  {
                response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );
            } 
            
            throw ex;
        }
            finally {

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime
            );

            int status = response.getStatus();

            String method = request.getMethod();

            // Do not log query strings, bodies or headers.
            String path = sanitizePath(
                    request.getRequestURI()
            );

            try {
                if (status >= 500) {

                    log.error(
                            "HTTP method={} path={} status={} "
                                    + "durationMs={} requestId={}",
                            method,
                            path,
                            status,
                            durationMs,
                            requestId
                    );

                } else if (status >= 400) {

                    log.warn(
                            "HTTP method={} path={} status={} "
                                    + "durationMs={} requestId={}",
                            method,
                            path,
                            status,
                            durationMs,
                            requestId
                    );

                } else {

                    log.info(
                            "HTTP method={} path={} status={} "
                                    + "durationMs={} requestId={}",
                            method,
                            path,
                            status,
                            durationMs,
                            requestId
                    );
                }

            } finally {
                // Prevent request data leaking between
                // reused application threads.
                MDC.remove(REQUEST_ID);
            }
        }
    }

    // Avoid control characters and oversized URI log entries.
    private String sanitizePath(String path) {

        if (path == null) {
            return "unknown";
        }

        String sanitized = path.replaceAll(
                "[\\r\\n\\t\\p{Cntrl}]",
                "_"
        );

        if (sanitized.length() > 200) {
            return sanitized.substring(0, 200);
        }

        return sanitized;
    }
}
