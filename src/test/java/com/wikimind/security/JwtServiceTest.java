package com.wikimind.security;

import com.wikimind.model.AppUser;
import com.wikimind.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);
    private final AppUser user = new AppUser("alice", "hash", Role.MEMBER, "acme");

    @Test
    void tokenRoundTripKeepsUsernameRoleAndOrg() {
        Claims claims = jwtService.parse(jwtService.generateToken(user));

        assertEquals("alice", claims.getSubject());
        assertEquals("MEMBER", claims.get("role", String.class));
        assertEquals("acme", claims.get("orgId", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.parse(token + "x"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-of-32-plus-chars", 60_000);

        assertThrows(JwtException.class, () -> jwtService.parse(other.generateToken(user)));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000);

        assertThrows(JwtException.class, () -> jwtService.parse(shortLived.generateToken(user)));
    }
}
