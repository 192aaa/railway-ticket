package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(2)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    /** 显式放行的公开 /api 路径；其余 /api 路径一律要求登录。 */
    private static final List<String> PUBLIC_API_PREFIXES = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/refresh",
            "/api/ticket/",
            "/api/station/",
            "/api/train/",
            "/api/dashboard/"
    );

    public JwtAuthFilter(JwtUtil jwtUtil, TokenService tokenService) {
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // 非 /api 路径（actuator、swagger、静态资源、/error）不在此拦截
        if (!path.startsWith("/api/") || isPublicApi(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "请先登录");
            return;
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token) || tokenService.isBlacklisted(token)) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return;
        }

        request.setAttribute("userId", jwtUtil.getUserId(token));
        filterChain.doFilter(request, response);
    }

    private boolean isPublicApi(String path) {
        for (String prefix : PUBLIC_API_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":401,\"message\":\"" + safeMessage + "\",\"data\":null}");
    }
}
