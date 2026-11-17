package com.example.demo.config;

import brave.Tracing;
import brave.handler.SpanHandler;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * Spring Boot 4 不再自动配置 Micrometer Tracing，这里手动装配 Brave Tracer 与 Zipkin 上报。
 * 未配置 ZIPKIN_ENDPOINT 时仍创建 Tracer（span 只用于日志 traceId，不导出）。
 */
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    @Bean
    public Tracer tracer(
            @Value("${spring.application.name:railway-ticket}") String serviceName,
            @Value("${management.zipkin.tracing.endpoint:}") String endpoint,
            @Value("${management.tracing.sampling.probability:1.0}") double sampling) {
        Sampler sampler = sampling >= 1.0
                ? Sampler.ALWAYS_SAMPLE
                : Sampler.create((float) sampling);

        Tracing.Builder builder = Tracing.newBuilder()
                .localServiceName(serviceName)
                .sampler(sampler);

        if (endpoint != null && !endpoint.isBlank()) {
            Sender sender = URLConnectionSender.newBuilder()
                    .endpoint(endpoint)
                    .connectTimeout(1000)
                    .readTimeout(1000)
                    .build();
            Reporter<zipkin2.Span> reporter = AsyncReporter.builder(sender).build();
            SpanHandler handler = ZipkinSpanHandler.create(reporter);
            builder.addSpanHandler(handler);
            log.info("Zipkin 链路追踪已启用，上报地址: {}", endpoint);
        } else {
            log.info("链路追踪已启用（日志 traceId），未配置 ZIPKIN_ENDPOINT，不导出 Span");
        }

        Tracing tracing = builder.build();
        return new BraveTracer(tracing.tracer(), new BraveCurrentTraceContext(tracing.currentTraceContext()));
    }
}
