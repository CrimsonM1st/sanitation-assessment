package com.example.sanitationassessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.sanitationassessment.dto.auth.LoginRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.entity.SystemUserEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.mapper.SystemUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final SystemUserMapper systemUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(SystemUserMapper systemUserMapper, PasswordEncoder passwordEncoder) {
        this.systemUserMapper = systemUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public SystemUserResponse authenticate(LoginRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();

        SystemUserEntity systemUserEntity = systemUserMapper.selectOne(
                Wrappers.<SystemUserEntity>lambdaQuery()
                        .eq(SystemUserEntity::getUsername, username)
        );

        if (systemUserEntity == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(systemUserEntity.getEnabled())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, systemUserEntity.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        return new SystemUserResponse(
                systemUserEntity.getId(),
                systemUserEntity.getUsername(),
                systemUserEntity.getRole(),
                systemUserEntity.getEnabled(),
                systemUserEntity.getCreatedAt()
        );
    }
}
