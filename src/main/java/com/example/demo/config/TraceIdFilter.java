package com.example.demo.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个 HTTP 请求分配/透传 traceId，写入 MDC 与响应头，使日志可按请求关联检索。
 * 启用链路追踪时（存在 Tracer bean）同时创建 SERVER span，日志与 Zipkin 共用同一 traceId；
 * 入站 X-Trace-Id 作为 span tag 保留，便于跨系统关联。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private final ObjectProvider<Tracer> tracerProvider;

    public TraceIdFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String sanitizedIncoming = sanitize(incoming);

        Tracer tracer = tracerProvider.getIfAvailable();
        Span span = null;
        Tracer.SpanInScope scope = null;

        try {
            if (tracer != null) {
                span = tracer.nextSpan().name(request.getMethod() + " " + request.getRequestURI()).start();
                scope = tracer.withSpan(span);
                if (sanitizedIncoming != null && !sanitizedIncoming.isEmpty()) {
                    span.tag("x.trace.id", sanitizedIncoming);
                }
                MDC.put(MDC_KEY, span.context().traceId());
                response.setHeader(HEADER, span.context().traceId());
            } else {
                String traceId = sanitizedIncoming != null && !sanitizedIncoming.isEmpty()
                        ? sanitizedIncoming
                        : UUID.randomUUID().toString().replace("-", "");
                MDC.put(MDC_KEY, traceId);
                response.setHeader(HEADER, traceId);
            }
            filterChain.doFilter(request, response);
        } finally {
            if (scope != null) scope.close();
            if (span != null) span.end();
            MDC.remove(MDC_KEY);
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.replaceAll("[^a-zA-Z0-9-]", "");
        return cleaned.substring(0, Math.min(32, cleaned.length()));
    }
}
