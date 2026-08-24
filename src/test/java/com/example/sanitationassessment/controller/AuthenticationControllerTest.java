package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.auth.LoginRequest;
import com.example.sanitationassessment.dto.auth.LoginResponse;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void validRequestShouldReturnLoginResponse() throws Exception {
        String requestBody = """
                {
                  "username": "admin",
                  "password": "password123"
                }
                """;
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );

        LoginResponse loginResponse = new LoginResponse(
                "test-access-token",
                "Bearer",
                user
        );

        when(authenticationService.authenticate(any(LoginRequest.class)))
                .thenReturn(loginResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("test-access-token"))
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    void blankUsernameShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "username": "",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("用户名不能为空"));
        verify(authenticationService, never()).authenticate(any(LoginRequest.class));
    }

    @Test
    void blankPasswordShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "username": "admin",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("密码不能为空"));
        verify(authenticationService, never()).authenticate(any(LoginRequest.class));
    }
}
