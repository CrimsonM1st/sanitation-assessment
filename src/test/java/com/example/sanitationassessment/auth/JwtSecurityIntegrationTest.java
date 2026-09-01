package com.example.sanitationassessment.auth;

import com.example.sanitationassessment.config.JwtProperties;
import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.dto.assessment.QueryAssessmentTaskRequest;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.service.AssessmentTaskService;
import com.example.sanitationassessment.service.SystemUserService;
import com.example.sanitationassessment.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AssessmentTaskService assessmentTaskService;

    @MockitoBean
    private SystemUserService systemUserService;

    @MockitoBean
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void protectedEndpointWithoutTokenShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(get("/assessment-tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg")
                        .value("未登录或Token无效"));
        verifyNoInteractions(assessmentTaskService);
    }

    @Test
    void protectedEndpointWithValidTokenShouldReturnSuccess() throws Exception {
        when(assessmentTaskService.query(
                any(QueryAssessmentTaskRequest.class)))
                .thenReturn(new PageResult<>(
                        List.of(), 0, 1, 10, 0
                ));

        SystemUserResponse user = new SystemUserResponse(
                1L,
                "inspector",
                UserRole.INSPECTOR,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/assessment-tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assessmentTaskService)
                .query(any(QueryAssessmentTaskRequest.class));
    }

    @Test
    void tamperedTokenShouldReturnUnauthorized() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "inspector",
                UserRole.INSPECTOR,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);

        String[] parts = token.split("\\.");
        String signature = parts[2];

        parts[2] = (signature.startsWith("A") ? "B" : "A")
                + signature.substring(1);

        String tamperedToken = String.join(".", parts);

        mockMvc.perform(get("/assessment-tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tamperedToken
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg")
                        .value("未登录或Token无效"));

        verifyNoInteractions(assessmentTaskService);
    }

    @Test
    void inspectorCreateUserShouldReturnForbidden() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "inspector",
                UserRole.INSPECTOR,
                true,
                LocalDateTime.now()
        );
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-user",
                                  "password": "password123",
                                  "role": "INSPECTOR"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("权限不足"));

        verifyNoInteractions(systemUserService);
    }

    @Test
    void adminCreateUserShouldReturnOk() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );
        String token = jwtTokenProvider.generateToken(user);

        when(systemUserService.create(
                any(CreateSystemUserRequest.class)))
                .thenReturn(new SystemUserResponse(
                        2L,
                        "new-user",
                        UserRole.INSPECTOR,
                        true,
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-user",
                                  "password": "password123",
                                  "role": "INSPECTOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("new-user"));


        verify(systemUserService, times(1))
                .create(any(CreateSystemUserRequest.class));

    }

    @Test
    void expiredTokenShouldReturnUnauthorized() throws Exception {
        JwtTokenProvider expiredTokenProvider =
                new JwtTokenProvider(
                        new JwtProperties(
                                jwtProperties.secret(),
                                Duration.ofSeconds(-1)
                        )
                );
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );
        String token = expiredTokenProvider.generateToken(user);
        mockMvc.perform(get("/assessment-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("未登录或Token无效"));

        verifyNoInteractions(assessmentTaskService);
    }

    @Test
    void currentUserEndpointShouldReturnUserWithoutInternalTokenFields() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "inspector",
                UserRole.INSPECTOR,
                true,
                LocalDateTime.now()
        );
        String token = jwtTokenProvider.generateToken(user);
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("inspector"))
                .andExpect(jsonPath("$.data.role").value("INSPECTOR"))
                .andExpect(jsonPath("$.data.tokenId").doesNotExist())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());

    }

    @Test
    void revokedTokenShouldReturnUnauthorized() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );
        String token = jwtTokenProvider.generateToken(user);
        when(jwtTokenBlacklist.isRevoked(anyString())).thenReturn(true);

        mockMvc.perform(get("/assessment-tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(assessmentTaskService);
    }

    @Test
    void logoutWithoutTokenShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(jwtTokenBlacklist, never())
                .revoke(anyString(), any(Instant.class));
    }

    @Test
    void logoutWithValidTokenShouldRevokeCurrentJti() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);

        AuthenticatedUser authenticatedUser =
                jwtTokenProvider.parseToken(token);

        mockMvc.perform(post("/auth/logout")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("退出成功"));

        verify(jwtTokenBlacklist)
                .revoke(
                        authenticatedUser.tokenId(),
                        authenticatedUser.expiresAt()
                );
    }

    @Test
    void redisUnavailableDuringBlacklistCheckShouldReturnServiceUnavailable() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);
        when(jwtTokenBlacklist.isRevoked(anyString())).thenThrow(
                RedisConnectionFailureException.class);

        mockMvc.perform(get("/assessment-tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.msg").value("认证服务暂时不可用"));

        verifyNoInteractions(assessmentTaskService);
    }

    @Test
    void inspectorCreateAssessmentTaskShouldReturnForbidden() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "inspector",
                UserRole.INSPECTOR,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(post("/assessment-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "姑苏区环卫一部",
                                  "status": "PENDING",
                                  "score": null
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("权限不足"));
        verifyNoInteractions(assessmentTaskService);
    }

    @Test
    void adminCreateAssessmentTaskShouldReturnSuccess() throws Exception {
        SystemUserResponse user = new SystemUserResponse(
                1L,
                "admin",
                UserRole.ADMIN,
                true,
                LocalDateTime.now()
        );

        String token = jwtTokenProvider.generateToken(user);

        when(assessmentTaskService.create(any())).thenReturn(new AssessmentTask(
                1L,
                "姑苏区环卫一部",
                TaskStatus.PENDING,
                null,
                LocalDateTime.now()
        ));

        mockMvc.perform(post("/assessment-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "姑苏区环卫一部",
                                  "status": "PENDING",
                                  "score": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.departmentName").value("姑苏区环卫一部"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        verify(assessmentTaskService)
                .create(any(CreateAssessmentTaskRequest.class));
    }
}
