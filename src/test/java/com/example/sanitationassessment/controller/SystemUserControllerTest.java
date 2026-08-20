package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.service.SystemUserService;
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
public class SystemUserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SystemUserService systemUserService;

    @Test
    void shouldCreateUser() throws Exception {
        when(systemUserService.create(any(CreateSystemUserRequest.class)))
                .thenReturn(new SystemUserResponse(
                        1L,
                        "admin",
                        UserRole.ADMIN,
                        true,
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "password123",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void passwordTooShortShouldNotCreateUser() throws Exception {
        String requestBody = """
                {
                  "username": "admin",
                  "password": "123",
                  "role": "ADMIN"
                }
                """;
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("密码长度必须在8到72之间"));
        verify(systemUserService, never())
                .create(any(CreateSystemUserRequest.class));
    }

    @Test
    void shouldNotCreateUserWhenRoleIsEmpty() throws Exception {
        String requestBody = """
                {
                  "username": "admin",
                  "password": "password123"
                }
                """;
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("角色不能为空"));
        verify(systemUserService, never())
                .create(any(CreateSystemUserRequest.class));

    }
}
