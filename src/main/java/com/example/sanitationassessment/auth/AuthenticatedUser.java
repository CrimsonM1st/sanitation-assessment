package com.example.sanitationassessment.auth;

import com.example.sanitationassessment.domain.UserRole;

public record AuthenticatedUser(Long userId,
                                String username,
                                UserRole role) {
}
