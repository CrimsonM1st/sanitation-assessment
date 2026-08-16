package com.example.sanitationassessment.service;

import com.example.sanitationassessment.cache.AssessmentTaskCache;
import com.example.sanitationassessment.cache.AssessmentTaskCacheResult;
import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.UpdateAssessmentTaskStatusRequest;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import com.example.sanitationassessment.event.AssessmentTaskUpdatedEvent;
import com.example.sanitationassessment.exception.ConcurrentUpdateException;
import com.example.sanitationassessment.exception.TaskNotFoundException;
import com.example.sanitationassessment.mapper.AssessmentTaskAuditLogMapper;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AssessmentTaskServiceTest {

    @Mock
    private AssessmentTaskMapper assessmentTaskMapper;

    @Mock
    private AssessmentTaskAuditLogMapper assessmentTaskAuditLogMapper;

    @Mock
    private AssessmentTaskCache assessmentTaskCache;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        verify(eventPublisher, never()).publishEvent(any(AssessmentTaskUpdatedEvent.class));
    }

    @Test
    void findByIdShouldReturnCachedTaskWhenCacheHits() {
        AssessmentTask assessmentTask = new AssessmentTask();
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.hit(assessmentTask));
        AssessmentTask byId = assessmentTaskService.findById(1L);
        verifyNoInteractions(assessmentTaskMapper);
        assertSame(assessmentTask, byId);
    }

    @Test
    void findByIdShouldLoadDatabaseAndCacheWhenCacheMisses() {
        AssessmentTaskEntity entity = new AssessmentTaskEntity();
        entity.setId(1L);
        entity.setStatus(TaskStatus.PROCESSING);
        entity.setVersion(0);
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.miss());
        when(assessmentTaskMapper.selectById(1L))
                .thenReturn(entity);
        AssessmentTask result = assessmentTaskService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals(TaskStatus.PROCESSING, result.getStatus());

        verify(assessmentTaskCache).get(1L);
        verify(assessmentTaskMapper).selectById(1L);
        verify(assessmentTaskCache).put(result);
    }

    @Test
    void updateStatusShouldPublishEventWhenUpdateSucceeds() {
        AssessmentTaskEntity assessmentTaskEntity = new AssessmentTaskEntity();
        assessmentTaskEntity.setId(1L);
        assessmentTaskEntity.setStatus(TaskStatus.PROCESSING);
        assessmentTaskEntity.setVersion(0);
        when(assessmentTaskMapper.selectById(1L))
                .thenReturn(assessmentTaskEntity);
        when(assessmentTaskMapper.updateById(
                any(AssessmentTaskEntity.class)))
                .thenReturn(1);
        UpdateAssessmentTaskStatusRequest request =
                new UpdateAssessmentTaskStatusRequest();
        request.setStatus(TaskStatus.COMPLETED);
        request.setScore(90);
        AssessmentTask result =
                assessmentTaskService.updateStatus(1L, request);

        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertEquals(90, result.getScore());

        verify(assessmentTaskMapper)
                .updateById(any(AssessmentTaskEntity.class));
        verify(eventPublisher).publishEvent(any(AssessmentTaskUpdatedEvent.class));
    }

    @Test
    void findByIdShouldNotQueryDatabaseWhenNullCacheHits() {
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.hit(null));

        assertThrows(
                TaskNotFoundException.class,
                () -> assessmentTaskService.findById(1L)
        );

        verifyNoInteractions(assessmentTaskMapper);
    }

    @Test
    void findByIdShouldCacheNullWhenDatabaseTaskDoesNotExist() {
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.miss());
        when(assessmentTaskMapper.selectById(1L))
                .thenReturn(null);

        assertThrows(
                TaskNotFoundException.class,
                () -> assessmentTaskService.findById(1L)
        );

        verify(assessmentTaskMapper).selectById(1L);
        verify(assessmentTaskCache).putNull(1L);
    }
}

