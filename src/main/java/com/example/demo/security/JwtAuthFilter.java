package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final List<String> AUTH_REQUIRED_PREFIXES = List.of(
            "/api/order/",
            "/api/user/info",
            "/api/user/change-password",
            "/api/user/profile",
            "/api/message/"
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/ticket/",
            "/api/station/",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/error"
    );

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAuthRequiredPath(path)) {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                writeUnauthorized(response, "请先登录");
                return;
            }
            String token = header.substring(7);
            if (!jwtUtil.validateToken(token)) {
                writeUnauthorized(response, "登录已失效，请重新登录");
                return;
            }
            Long userId = jwtUtil.getUserId(token);
            request.setAttribute("userId", userId);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean isAuthRequiredPath(String path) {
        for (String prefix : AUTH_REQUIRED_PREFIXES) {
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