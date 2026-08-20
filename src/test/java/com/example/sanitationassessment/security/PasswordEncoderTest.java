package com.example.sanitationassessment.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Test
    void samePasswordShouldGenerateDifferentHashes() {
        String first = passwordEncoder.encode("password123");
        String second = passwordEncoder.encode("password123");

        assertNotEquals(first, second);
    }

    @Test
    void matchesShouldVerifyPassword() {
        String encodedPassword =
                passwordEncoder.encode("password123");

        assertTrue(passwordEncoder.matches(
                "password123", encodedPassword));
        assertFalse(passwordEncoder.matches(
                "wrong-password", encodedPassword));
    }
}