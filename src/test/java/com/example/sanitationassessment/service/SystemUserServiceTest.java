package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.entity.SystemUserEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

    @Mock
    private SystemUserMapper systemUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemUserService systemUserService;

    @Test
    void createShouldEncodePasswordAndSaveUser() {
        CreateSystemUserRequest request =
                new CreateSystemUserRequest(
                        " admin ",
                        "password123",
                        UserRole.ADMIN
                );

        when(systemUserMapper.selectCount(any()))
                .thenReturn(0L);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(systemUserMapper.insert(
                any(SystemUserEntity.class)
        )).thenAnswer(invocation -> {
            SystemUserEntity entity =
                    invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });

        SystemUserResponse response =
                systemUserService.create(request);

        ArgumentCaptor<SystemUserEntity> captor =
                ArgumentCaptor.forClass(
                        SystemUserEntity.class
                );

        verify(systemUserMapper).insert(
                captor.capture()
        );

        SystemUserEntity saved = captor.getValue();

        assertEquals("admin", saved.getUsername());
        assertEquals(
                "encoded-password",
                saved.getPasswordHash()
        );
        assertNotEquals(
                "password123",
                saved.getPasswordHash()
        );
        assertEquals(UserRole.ADMIN, saved.getRole());
        assertTrue(saved.getEnabled());
        assertNotNull(saved.getCreatedAt());
        assertEquals(
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );

        assertEquals(1L, response.getId());
        assertEquals("admin", response.getUsername());
        assertEquals(UserRole.ADMIN, response.getRole());

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void whenUsernameDuplicatedReturnUserHasExist() {
        CreateSystemUserRequest request =
                new CreateSystemUserRequest(
                        " admin ",
                        "password123",
                        UserRole.ADMIN
                );
        when(systemUserMapper.selectCount(any()))
                .thenReturn(1L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> systemUserService.create(request)
        );
        assertEquals("用户名已存在", exception.getMessage());

        verifyNoInteractions(passwordEncoder);

        verify(systemUserMapper, never())
                .insert(any(SystemUserEntity.class));
    }

    @Test
    void concurrentUniqueKeyDuplicate() {
        when(systemUserMapper.selectCount(any()))
                .thenReturn(0L);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");
        when(systemUserMapper.insert(
                any(SystemUserEntity.class)
        )).thenThrow(
                new DuplicateKeyException("duplicate username")
        );

        CreateSystemUserRequest request =
                new CreateSystemUserRequest(
                        " admin ",
                        "password123",
                        UserRole.ADMIN
                );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> systemUserService.create(request)
        );

        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void insertAffectZero() {
        when(systemUserMapper.selectCount(any()))
                .thenReturn(0L);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");
        when(systemUserMapper.insert(any(SystemUserEntity.class)))
                .thenReturn(0);

        CreateSystemUserRequest request =
                new CreateSystemUserRequest(
                        " admin ",
                        "password123",
                        UserRole.ADMIN
                );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> systemUserService.create(request)
        );
        assertEquals("创建用户失败", exception.getMessage());
    }
}