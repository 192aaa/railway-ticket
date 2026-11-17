package com.example.demo.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceIdFilterTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    private final TraceIdFilter filter = new TraceIdFilter(tracerProvider);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ticket/query");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new org.springframework.mock.web.MockFilterChain());

        String traceId = response.getHeader(TraceIdFilter.HEADER);
        assertThat(traceId).isNotBlank().hasSize(32).matches("[0-9a-f]+");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull(); // 请求结束后已清理
    }

    @Test
    void reusesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ticket/query");
        request.addHeader(TraceIdFilter.HEADER, "upstream-trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) ->
                assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isEqualTo("upstream-trace-123");
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("upstream-trace-123");
    }

    @Test
    void sanitizesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ticket/query");
        request.addHeader(TraceIdFilter.HEADER, "abc<script>!!123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new org.springframework.mock.web.MockFilterChain());

        String traceId = response.getHeader(TraceIdFilter.HEADER);
        assertThat(traceId).isNotBlank().matches("[a-zA-Z0-9-]+");
        assertThat(traceId).doesNotContain("<", ">", "!");
    }

    @Test
    void mdcIsClearedEvenWhenChainThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ticket/query");
        FilterChain chain = (req, res) -> {
            assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNotBlank();
            throw new RuntimeException("boom");
        };
        try {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        } catch (RuntimeException ignored) {
            // 预期异常向上传播
        }
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
