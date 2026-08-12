package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.UpdateAssessmentTaskStatusRequest;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import com.example.sanitationassessment.exception.ConcurrentUpdateException;
import com.example.sanitationassessment.mapper.AssessmentTaskAuditLogMapper;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AssessmentTaskServiceTest {

    @Mock
    private AssessmentTaskMapper assessmentTaskMapper;

    @Mock
    private AssessmentTaskAuditLogMapper assessmentTaskAuditLogMapper;

    @InjectMocks
    private AssessmentTaskService assessmentTaskService;

    @Test
    void updateStatusShouldThrowConcurrentUpdateExceptionWhenUpdateReturnsZero() {
        AssessmentTaskEntity entity = new AssessmentTaskEntity();
        entity.setId(1L);
        entity.setStatus(TaskStatus.PROCESSING);
        entity.setVersion(0);
        when(assessmentTaskMapper.selectById(1L))
                .thenReturn(entity);
        when(assessmentTaskMapper.updateById(
                any(AssessmentTaskEntity.class)))
                .thenReturn(0);

        UpdateAssessmentTaskStatusRequest request =
                new UpdateAssessmentTaskStatusRequest();

        request.setStatus(TaskStatus.COMPLETED);
        request.setScore(90);
        ConcurrentUpdateException exception = assertThrows(
                ConcurrentUpdateException.class,
                () -> assessmentTaskService.updateStatus(1L, request)
        );
        assertEquals(
                "任务已被其他请求修改，请刷新后重试",
                exception.getMessage()
        );

        verify(assessmentTaskMapper).selectById(1L);
        verify(assessmentTaskMapper)
                .updateById(any(AssessmentTaskEntity.class));
    }
}

