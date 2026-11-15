package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebConfigTest {

    @Test
    void shouldUseConfiguredOriginWhitelist() {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins",
                "https://fe.example.com, https://admin.example.com");

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class, Answers.RETURNS_SELF);
        when(registry.addMapping(anyString())).thenReturn(registration);

        config.addCorsMappings(registry);

        verify(registration).allowedOriginPatterns("https://fe.example.com", "https://admin.example.com");
    }

    @Test
    void shouldFallbackToWildcardWhenBlank() {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "  ,  ");

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class, Answers.RETURNS_SELF);
        when(registry.addMapping(anyString())).thenReturn(registration);

        config.addCorsMappings(registry);

        verify(registration).allowedOriginPatterns("*");
    }
}
