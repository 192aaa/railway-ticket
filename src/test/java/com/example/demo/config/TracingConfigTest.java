package com.example.demo.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TracingConfigTest {

    @Autowired
    private Tracer tracer;

    @Test
    void tracerBeanIsConfigured() {
        assertThat(tracer).isNotNull();
    }

    @Test
    void createdSpanHasTraceId() {
        Span span = tracer.nextSpan().name("test-span").start();
        try {
            assertThat(span.context().traceId()).isNotBlank();
            assertThat(span.context().spanId()).isNotBlank();
        } finally {
            span.end();
        }
    }
}
