package com.example.sanitationassessment.service;

import com.example.sanitationassessment.cache.AssessmentTaskCache;
import com.example.sanitationassessment.cache.AssessmentTaskCacheResult;
import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.dto.assessment.UpdateAssessmentTaskStatusRequest;
import com.example.sanitationassessment.entity.AssessmentTaskAuditLogEntity;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import com.example.sanitationassessment.event.AssessmentTaskChangedEvent;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.exception.CacheRebuildBusyException;
import com.example.sanitationassessment.exception.ConcurrentUpdateException;
import com.example.sanitationassessment.exception.TaskNotFoundException;
import com.example.sanitationassessment.lock.RedisLock;
import com.example.sanitationassessment.mapper.AssessmentTaskAuditLogMapper;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;

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

    @Mock
    private RedisLock redisLock;

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
        verify(eventPublisher, never()).publishEvent(any(AssessmentTaskChangedEvent.class));
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
        when(redisLock.tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        )).thenReturn("token-1");
        AssessmentTask result = assessmentTaskService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals(TaskStatus.PROCESSING, result.getStatus());

        verify(assessmentTaskCache, times(2)).get(1L);
        verify(assessmentTaskMapper).selectById(1L);
        verify(assessmentTaskCache).put(result);
        verify(redisLock).unlock(
                "sanitation:lock:assessment-task:1",
                "token-1"
        );
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
        verify(eventPublisher).publishEvent(any(AssessmentTaskChangedEvent.class));
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
        when(redisLock.tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        )).thenReturn("token-1");
        assertThrows(
                TaskNotFoundException.class,
                () -> assessmentTaskService.findById(1L)
        );

        verify(assessmentTaskMapper).selectById(1L);
        verify(assessmentTaskCache).putNull(1L);
        verify(redisLock).unlock(
                "sanitation:lock:assessment-task:1",
                "token-1"
        );
    }

    @Test
    void whenCreateSuccessShouldPublishEvent() {
        when(assessmentTaskMapper.insert(
                any(AssessmentTaskEntity.class)))
                .thenAnswer(invocation -> {
                    AssessmentTaskEntity insertedEntity =
                            invocation.getArgument(0);
                    insertedEntity.setId(1L);
                    return 1;
                });
        when(assessmentTaskAuditLogMapper.insert(
                any(AssessmentTaskAuditLogEntity.class)))
                .thenReturn(1);
        CreateAssessmentTaskRequest request =
                new CreateAssessmentTaskRequest();
        AssessmentTask result =
                assessmentTaskService.create(request);

        verify(assessmentTaskMapper)
                .insert(any(AssessmentTaskEntity.class));
        verify(eventPublisher).publishEvent(
                new AssessmentTaskChangedEvent(1L)
        );
    }

    @Test
    void whenCreateRollbackShouldNotPublishEvent() {
        AssessmentTaskEntity assessmentTaskEntity = new AssessmentTaskEntity();
        assessmentTaskEntity.setId(1L);
        assessmentTaskEntity.setStatus(TaskStatus.PROCESSING);
        assessmentTaskEntity.setVersion(0);
        when(assessmentTaskMapper.insert(
                any(AssessmentTaskEntity.class)))
                .thenReturn(1);

        when(assessmentTaskAuditLogMapper.insert(
                any(AssessmentTaskAuditLogEntity.class)))
                .thenReturn(0);
        CreateAssessmentTaskRequest request =
                new CreateAssessmentTaskRequest();

        BusinessException businessException = assertThrows(BusinessException.class,
                () -> assessmentTaskService.create(request));
        verify(eventPublisher, never()).publishEvent(any(AssessmentTaskChangedEvent.class));

    }

    @Test
    void doubleCheckTest() {
        AssessmentTask task = new AssessmentTask();

        when(assessmentTaskCache.get(1L))
                .thenReturn(
                        AssessmentTaskCacheResult.miss(),
                        AssessmentTaskCacheResult.hit(task)
                );

        when(redisLock.tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        )).thenReturn("token-1");

        AssessmentTask result =
                assessmentTaskService.findById(1L);

        assertSame(task, result);
        verifyNoInteractions(assessmentTaskMapper);
        verify(redisLock).unlock(
                "sanitation:lock:assessment-task:1",
                "token-1"
        );
    }

    @Test
    void redisLockCompetitiveFailed() {
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.miss());

        when(redisLock.tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        )).thenReturn(null);

        CacheRebuildBusyException exception = assertThrows(
                CacheRebuildBusyException.class,
                () -> assessmentTaskService.findById(1L)
        );

        assertEquals(
                "缓存正在重建，请稍后重试",
                exception.getMessage()
        );

        verifyNoInteractions(assessmentTaskMapper);
        verify(redisLock, never()).unlock(
                anyString(),
                anyString()
        );
    }

    @Test
    void findByIdShouldReturnCacheAfterWaitingWhenLockFails() {
        AssessmentTask task = new AssessmentTask();

        when(assessmentTaskCache.get(1L))
                .thenReturn(
                        AssessmentTaskCacheResult.miss(),
                        AssessmentTaskCacheResult.hit(task)
                );

        when(redisLock.tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        )).thenReturn(null);

        AssessmentTask result =
                assessmentTaskService.findById(1L);

        assertSame(task, result);

        verify(redisLock).tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        );
        verifyNoInteractions(assessmentTaskMapper);
        verify(redisLock, never()).unlock(
                anyString(),
                anyString()
        );
    }

    @Test
    void findByIdShouldGetLockAndRebuildAfterFirstLockFails() {
        AssessmentTaskEntity assessmentTaskEntity = new AssessmentTaskEntity();
        assessmentTaskEntity.setId(1L);
        assessmentTaskEntity.setStatus(TaskStatus.PROCESSING);
        assessmentTaskEntity.setVersion(0);
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.miss());

        when(redisLock.tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        )).thenReturn(null, "token-1");

        when(assessmentTaskMapper.selectById(1L))
                .thenReturn(assessmentTaskEntity);

        AssessmentTask result =
                assessmentTaskService.findById(1L);

        assertEquals(1L, result.getId());

        verify(redisLock, times(2)).tryLock(
                eq("sanitation:lock:assessment-task:1"),
                any(Duration.class)
        );

        verify(assessmentTaskMapper).selectById(1L);
        verify(assessmentTaskCache).put(result);

        verify(redisLock).unlock(
                "sanitation:lock:assessment-task:1",
                "token-1"
        );
    }
}

