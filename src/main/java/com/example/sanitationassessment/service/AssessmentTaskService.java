package com.example.sanitationassessment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.dto.assessment.QueryAssessmentTaskRequest;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.exception.TaskNotFoundException;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import com.example.sanitationassessment.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssessmentTaskService {
    private final AssessmentTaskMapper assessmentTaskMapper;

    public AssessmentTaskService(AssessmentTaskMapper assessmentTaskMapper, Mapper mapper) {
        this.assessmentTaskMapper = assessmentTaskMapper;
    }

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
        return toDomain(assessmentTaskEntity);
    }

    public AssessmentTask findById(Long id) {
        AssessmentTaskEntity assessmentTaskEntity = assessmentTaskMapper.selectById(id);
        if (assessmentTaskEntity == null) {
            throw new TaskNotFoundException("考评任务不存在，id=" + id);
        }
        return toDomain(assessmentTaskEntity);
    }

    private AssessmentTask toDomain(AssessmentTaskEntity assessmentTaskEntity) {
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
}
