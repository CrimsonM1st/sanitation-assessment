package com.example.sanitationassessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.entity.SystemUserEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.mapper.SystemUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SystemUserService {
    private final SystemUserMapper systemUserMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemUserService(SystemUserMapper systemUserMapper, PasswordEncoder passwordEncoder) {
        this.systemUserMapper = systemUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SystemUserResponse create(CreateSystemUserRequest request) {
        String username = request.getUsername().trim();
        Long count = systemUserMapper.selectCount(
                Wrappers.<SystemUserEntity>lambdaQuery()
                        .eq(SystemUserEntity::getUsername, username)
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SystemUserEntity systemUserEntity = new SystemUserEntity();
        systemUserEntity.setUsername(username);
        systemUserEntity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        systemUserEntity.setRole(request.getRole());
        systemUserEntity.setEnabled(true);
        LocalDateTime now = LocalDateTime.now();
        systemUserEntity.setCreatedAt(now);
        systemUserEntity.setUpdatedAt(now);
        int affectedRows;
        try {
            affectedRows = systemUserMapper.insert(
                    systemUserEntity
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("用户名已存在");
        }
        if (affectedRows != 1) {
            throw new BusinessException("创建用户失败");
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
