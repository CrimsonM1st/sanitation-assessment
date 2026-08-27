package com.example.sanitationassessment.auth;

import com.example.sanitationassessment.domain.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public record AuthenticatedUser(Long userId,
                                String username,
                                UserRole role,
                                @JsonIgnore String tokenId,
                                @JsonIgnore Instant expiresAt) {
}
