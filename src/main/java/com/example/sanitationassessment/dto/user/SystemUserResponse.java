package com.example.sanitationassessment.dto.user;

import com.example.sanitationassessment.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemUserResponse {
    private Long id;
    private String username;
    private UserRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
