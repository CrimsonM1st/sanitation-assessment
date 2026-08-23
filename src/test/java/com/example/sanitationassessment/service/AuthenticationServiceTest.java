package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.auth.LoginRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.entity.SystemUserEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private SystemUserMapper systemUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticationSuccess() {
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(1L);
        entity.setUsername("admin");
        entity.setPasswordHash("encoded-password");
        entity.setRole(UserRole.ADMIN);
        entity.setEnabled(true);
        entity.setCreatedAt(LocalDateTime.now());

        when(systemUserMapper.selectOne(any())).thenReturn(entity);
        when(passwordEncoder.matches(
                "password123", "encoded-password"))
                .thenReturn(true);

        SystemUserResponse response = authenticationService.authenticate(
                new LoginRequest(" admin ", "password123")
        );

        assertEquals(1L, response.getId());
        assertEquals("admin", response.getUsername());
        assertEquals(UserRole.ADMIN, response.getRole());
        assertTrue(response.getEnabled());
        verify(systemUserMapper).selectOne(any());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void userIsNotExist() {
        SystemUserEntity user = this.createUser(true);
        when(systemUserMapper.selectOne(any())).thenReturn(null);

        LoginRequest request = new LoginRequest(user.getUsername(), "password123");
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.authenticate(request)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void passwordWrong() {
        SystemUserEntity entity = this.createUser(true);
        when(systemUserMapper.selectOne(any())).thenReturn(entity);
        when(passwordEncoder.matches(
                "password123", "encoded-password"))
                .thenReturn(false);

        LoginRequest request = new LoginRequest(entity.getUsername(), "password123");
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.authenticate(request)
        );
        assertEquals("用户名或密码错误", exception.getMessage());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void disabledUserShouldFailWithoutCheckingPassword() {
        SystemUserEntity entity = this.createUser(false);
        when(systemUserMapper.selectOne(any())).thenReturn(entity);

        LoginRequest request = new LoginRequest(entity.getUsername(), "password123");
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.authenticate(request)
        );
        assertEquals("用户名或密码错误", exception.getMessage());

        verifyNoInteractions(passwordEncoder);

    }

    private SystemUserEntity createUser(boolean enabled) {
        // 创建并返回测试用户
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(1L);
        entity.setUsername("admin");
        entity.setPasswordHash("encoded-password");
        entity.setRole(UserRole.ADMIN);
        entity.setEnabled(enabled);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
