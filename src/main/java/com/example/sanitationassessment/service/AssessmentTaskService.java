package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.exception.TaskNotFoundException;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AssessmentTaskService {
    private final AssessmentTaskMapper assessmentTaskMapper;

    public AssessmentTaskService(AssessmentTaskMapper assessmentTaskMapper) {
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
}
