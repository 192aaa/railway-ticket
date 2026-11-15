package com.example.demo.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingResourceShouldReturn404() {
        NoResourceFoundException ex =
                new NoResourceFoundException(HttpMethod.GET, "/api/unknown", "No static resource");

        ResponseEntity<Result<Void>> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("请求的资源不存在");
    }

    @Test
    void unexpectedExceptionShouldStillBeHandledAs500() {
        Result<Void> result = handler.handleException(new IllegalStateException("boom"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("系统繁忙，请稍后再试");
    }
}
