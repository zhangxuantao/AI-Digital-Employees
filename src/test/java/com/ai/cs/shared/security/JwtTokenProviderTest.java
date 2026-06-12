package com.ai.cs.shared.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider("test-secret-key-for-unit-tests-only-32chars", 3600000);
    }

    @Test
    void createAndParseTokenShouldSucceed() {
        String token = tokenProvider.createToken(1L, "testuser", List.of("ROLE_USER"));
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = tokenProvider.parseToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("testuser", claims.get("username"));
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        String token = tokenProvider.createToken(1L, "testuser", List.of());
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid-token"));
    }

    @Test
    void tokenShouldContainPermissions() {
        List<String> permissions = List.of("ROLE_ADMIN", "PERM_READ");
        String token = tokenProvider.createToken(1L, "admin", permissions);
        Claims claims = tokenProvider.parseToken(token);
        List<String> extractedPermissions = claims.get("permissions", List.class);
        assertEquals(permissions, extractedPermissions);
    }
}
