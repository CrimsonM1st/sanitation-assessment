package com.example.sanitationassessment.auth;

import com.example.sanitationassessment.config.JwtProperties;
import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {
    private static final String SECRET =
            "dGVzdC1qd3Qtc2VjcmV0LW11c3QtYmUtYXQtbGVhc3QtMzItYnl0ZXM=";

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(
                    new JwtProperties(SECRET, Duration.ofHours(2))
            );

    @Test
    void generatedTokenShouldContainExpectedClaims() {
        SystemUserResponse response = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now());
        String token = jwtTokenProvider.generateToken(response);

        SecretKey key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(SECRET)
        );

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(3, token.split("\\.").length);
        assertEquals("admin", claims.getSubject());
        assertEquals(
                1L,
                ((Number) claims.get("userId")).longValue()
        );
        assertEquals("ADMIN", claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        long validitySeconds =
                claims.getExpiration().toInstant().getEpochSecond()
                        - claims.getIssuedAt().toInstant().getEpochSecond();

        assertEquals(7200, validitySeconds);
    }

    @Test
    void tamperedTokenShouldFailVerification() {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);
        String[] parts = token.split("\\.");

        String signature = parts[2];
        parts[2] = (signature.startsWith("A") ? "B" : "A")
                + signature.substring(1);

        String tamperedToken = String.join(".", parts);

        SecretKey key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(SECRET)
        );

        assertThrows(
                SignatureException.class,
                () -> Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(tamperedToken)
        );
    }
}
