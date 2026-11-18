package com.athron.questionnaire;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class GatewayAuthentication extends OncePerRequestFilter {
    private final byte[] apiKey;

    GatewayAuthentication(@Value("${athron.api-key}") String apiKey) {
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("Gateway API key must be configured");
        }
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String key = request.getHeader("X-API-Key");
        String subject = request.getHeader("X-Authenticated-User-Sub");
        if (key == null || !MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8))
                || subject == null || subject.isBlank() || subject.length() > 255) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        request.setAttribute("accountId", subject);
        chain.doFilter(request, response);
    }
}
