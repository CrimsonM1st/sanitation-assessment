package com.example.sanitationassessment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.QuerySystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.entity.SystemUserEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.mapper.SystemUserMapper;
import com.example.sanitationassessment.vo.PageResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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
        return this.toResponse(systemUserEntity);
    }

    public PageResult<SystemUserResponse> query(QuerySystemUserRequest request) {
        String username = StringUtils.hasText(request.getUsername())
                ? request.getUsername().trim()
                : null;
        Page<SystemUserEntity> page =
                new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<SystemUserEntity> wrapper = Wrappers.<SystemUserEntity>lambdaQuery()
                .likeRight(StringUtils.hasText(username), SystemUserEntity::getUsername,
                        username)
                .eq(
                        request.getRole() != null,
                        SystemUserEntity::getRole,
                        request.getRole()
                )
                .eq(
                        request.getEnabled() != null,
                        SystemUserEntity::getEnabled,
                        request.getEnabled()
                )
                .orderByDesc(SystemUserEntity::getCreatedAt)
                .orderByDesc(SystemUserEntity::getId);
        Page<SystemUserEntity> systemUserEntityPage = systemUserMapper.selectPage(page, wrapper);
        List<SystemUserResponse> records = systemUserEntityPage.getRecords()
                .stream()
                .map(this::toResponse).toList();
        return new PageResult<>(records,
                systemUserEntityPage.getTotal(), systemUserEntityPage.getCurrent(),
                systemUserEntityPage.getSize(), systemUserEntityPage.getPages());
    }

    private SystemUserResponse toResponse(
            SystemUserEntity entity) {
        return new SystemUserResponse(entity.getId(), entity.getUsername(),
                entity.getRole(), entity.getEnabled(), entity.getCreatedAt());
    }
}
