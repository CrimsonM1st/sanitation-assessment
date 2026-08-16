package com.example.sanitationassessment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sanitationassessment.cache.AssessmentTaskCache;
import com.example.sanitationassessment.cache.AssessmentTaskCacheResult;
import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.dto.assessment.QueryAssessmentTaskRequest;
import com.example.sanitationassessment.dto.assessment.UpdateAssessmentTaskStatusRequest;
import com.example.sanitationassessment.entity.AssessmentTaskAuditLogEntity;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import com.example.sanitationassessment.event.AssessmentTaskUpdatedEvent;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.exception.ConcurrentUpdateException;
import com.example.sanitationassessment.exception.TaskNotFoundException;
import com.example.sanitationassessment.mapper.AssessmentTaskAuditLogMapper;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import com.example.sanitationassessment.vo.PageResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssessmentTaskService {
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final AssessmentTaskAuditLogMapper assessmentTaskAuditLogMapper;
    private final AssessmentTaskCache assessmentTaskCache;
    private final ApplicationEventPublisher eventPublisher;

    public AssessmentTaskService(AssessmentTaskMapper assessmentTaskMapper, AssessmentTaskAuditLogMapper assessmentTaskAuditLogMapper, AssessmentTaskCache assessmentTaskCache, ApplicationEventPublisher eventPublisher) {
        this.assessmentTaskMapper = assessmentTaskMapper;
        this.assessmentTaskAuditLogMapper = assessmentTaskAuditLogMapper;
        this.assessmentTaskCache = assessmentTaskCache;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AssessmentTask create(CreateAssessmentTaskRequest request) {
        if (request.getStatus() == TaskStatus.COMPLETED && request.getScore() == null) {
            throw new BusinessException("已完成任务必须提供分数");
        }
        AssessmentTaskEntity assessmentTaskEntity = new AssessmentTaskEntity();
        assessmentTaskEntity.setDepartmentName(request.getDepartmentName());
        assessmentTaskEntity.setStatus(request.getStatus());
        assessmentTaskEntity.setScore(request.getScore());
        LocalDateTime now = LocalDateTime.now();
        assessmentTaskEntity.setCreatedAt(now);
        assessmentTaskEntity.setUpdatedAt(now);
        int affectedRows = assessmentTaskMapper.insert(assessmentTaskEntity);
        if (affectedRows != 1) {
            throw new BusinessException("创建考评任务失败");
        }
        //创建任务时写入审计日志
        AssessmentTaskAuditLogEntity assessmentTaskAuditLogEntity = new AssessmentTaskAuditLogEntity();
        assessmentTaskAuditLogEntity.setTaskId(assessmentTaskEntity.getId());
        assessmentTaskAuditLogEntity.setAction("CREATE");
        assessmentTaskAuditLogEntity.setDetail("创建考评任务");
        assessmentTaskAuditLogEntity.setCreatedAt(LocalDateTime.now());
        affectedRows = assessmentTaskAuditLogMapper.insert(assessmentTaskAuditLogEntity);
        if (affectedRows != 1) {
            throw new BusinessException("记录审计日志失败");
        }
        return toDomain(assessmentTaskEntity);
    }

    public AssessmentTask findById(Long id) {
        AssessmentTaskCacheResult assessmentTaskCacheResult = assessmentTaskCache.get(id);
        if (assessmentTaskCacheResult.hit() && assessmentTaskCacheResult.task() != null) {
            return assessmentTaskCacheResult.task();
        }
        if (assessmentTaskCacheResult.hit()) {
            throw new TaskNotFoundException(
                    "考评任务不存在，id=" + id
            );
        }
        AssessmentTaskEntity assessmentTaskEntity = assessmentTaskMapper.selectById(id);
        if (assessmentTaskEntity == null) {
            assessmentTaskCache.putNull(id);
            throw new TaskNotFoundException("考评任务不存在，id=" + id);
        }
        AssessmentTask domain = toDomain(assessmentTaskEntity);
        assessmentTaskCache.put(domain);
        return domain;
    }

    public AssessmentTask toDomain(AssessmentTaskEntity assessmentTaskEntity) {
        return new AssessmentTask(assessmentTaskEntity.getId(), assessmentTaskEntity.getDepartmentName(),
                assessmentTaskEntity.getStatus(), assessmentTaskEntity.getScore(), assessmentTaskEntity.getCreatedAt());
    }

    public PageResult<AssessmentTask> query(QueryAssessmentTaskRequest request) {
        Page<AssessmentTaskEntity> page =
                new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<AssessmentTaskEntity> wrapper = Wrappers.<AssessmentTaskEntity>lambdaQuery()
                .eq(StringUtils.hasText(request.getDepartmentName()), AssessmentTaskEntity::getDepartmentName,
                        request.getDepartmentName())
                .eq(request.getStatus() != null, AssessmentTaskEntity::getStatus, request.getStatus())
                .orderByDesc(AssessmentTaskEntity::getCreatedAt)
                .orderByDesc(AssessmentTaskEntity::getId);
        Page<AssessmentTaskEntity> assessmentTaskEntityPage = assessmentTaskMapper.selectPage(page, wrapper);
        List<AssessmentTask> records = assessmentTaskEntityPage.getRecords()
                .stream()
                .map(this::toDomain)
                .toList();
        return new PageResult<>(records,
                assessmentTaskEntityPage.getTotal(), assessmentTaskEntityPage.getCurrent(),
                assessmentTaskEntityPage.getSize(), assessmentTaskEntityPage.getPages());
    }

    @Transactional
    public AssessmentTask updateStatus(Long id, UpdateAssessmentTaskStatusRequest request) {
        if (request.getStatus() == TaskStatus.COMPLETED && request.getScore() == null) {
            throw new BusinessException("已完成考评任务必须提供分数");
        }
        if ((request.getStatus() == TaskStatus.PENDING || request.getStatus() ==
                TaskStatus.PROCESSING) && request.getScore() != null) {
            throw new BusinessException("待完成考评任务和处理中考评任务不需要提供分数");
        }
        AssessmentTaskEntity assessmentTaskEntity = assessmentTaskMapper.selectById(id);
        if (assessmentTaskEntity == null) {
            throw new TaskNotFoundException("未查询到该任务");
        }
        if (assessmentTaskEntity.getStatus() == TaskStatus.COMPLETED && request.getStatus() !=
                TaskStatus.COMPLETED) {
            throw new BusinessException("已完成考评任务不允许回退状态");
        }
        assessmentTaskEntity.setStatus(request.getStatus());
        assessmentTaskEntity.setScore(request.getScore());
        assessmentTaskEntity.setUpdatedAt(LocalDateTime.now());
        int affectedRows = assessmentTaskMapper.updateById(assessmentTaskEntity);

        if (affectedRows != 1) {
            throw new ConcurrentUpdateException("任务已被其他请求修改，请刷新后重试");
        }
        eventPublisher.publishEvent(
                new AssessmentTaskUpdatedEvent(id)
        );
        return toDomain(assessmentTaskEntity);

    }
}
