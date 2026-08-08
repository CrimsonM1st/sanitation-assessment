package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.exception.TaskNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AssessmentTaskService {
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final Map<Long, AssessmentTask> storage =
            new ConcurrentHashMap<>();

    public AssessmentTask create(CreateAssessmentTaskRequest request) {
        if (request.getStatus() == TaskStatus.COMPLETED && request.getScore() == null) {
            throw new BusinessException("已完成任务必须提供分数");
        }
        long id = idGenerator.incrementAndGet();
        LocalDateTime createdAt = LocalDateTime.now();
        AssessmentTask assessmentTask = new AssessmentTask(id, request.getDepartmentName(), request.getStatus(), request.getScore(), createdAt);
        storage.put(id, assessmentTask);
        return assessmentTask;
    }

    public AssessmentTask findById(Long id) {
        AssessmentTask assessmentTask = storage.get(id);
        if (assessmentTask == null) {
            throw new TaskNotFoundException("考评任务不存在，id=" + id);
        }
        return assessmentTask;
    }
}
