package com.example.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private TokenService tokenService;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        tokenService = mock(TokenService.class);
        filter = new JwtAuthFilter(jwtUtil, tokenService);
    }

    @Test
    void publicApiPassesWithoutToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/ticket/query"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void actuatorAndSwaggerPassWithoutToken() throws Exception {
        for (String path : new String[]{"/actuator/health", "/swagger-ui.html", "/v3/api-docs"}) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", path), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    @Test
    void unknownApiRequiresToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/user/info"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void validTokenSetsUserId() throws Exception {
        when(jwtUtil.validateToken("jwt")).thenReturn(true);
        when(jwtUtil.getUserId("jwt")).thenReturn(7L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/order/my");
        request.addHeader("Authorization", "Bearer jwt");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute("userId")).isEqualTo(7L);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void blacklistedTokenIsRejected() throws Exception {
        when(jwtUtil.validateToken("jwt")).thenReturn(true);
        when(tokenService.isBlacklisted("jwt")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/order/my");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
