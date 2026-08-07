package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentStatisticsServiceTest {

    private final AssessmentStatisticsService service = new AssessmentStatisticsService();

    private AssessmentTask task(long id, String department, TaskStatus status, Integer score, LocalDateTime createdAt) {
        return new AssessmentTask(id, department, status, score, createdAt);
    }

    @Test
    void shouldCountTasksByStatus() {
        List<AssessmentTask> tasks = List.of(task(1L, "一部", TaskStatus.PENDING, null, LocalDateTime.of(2026, 8, 7, 9, 0)), task(2L, "一部", TaskStatus.PENDING, null, LocalDateTime.of(2026, 8, 7, 10, 0)), task(3L, "二部", TaskStatus.COMPLETED, 90, LocalDateTime.of(2026, 8, 7, 11, 0)));

        Map<TaskStatus, Long> result = service.countByStatus(tasks);

        assertEquals(2L, result.get(TaskStatus.PENDING));
        assertEquals(1L, result.get(TaskStatus.COMPLETED));
    }

    @Test
    void nullTasksShouldReturnEmptyStatusCounts() {
        Map<TaskStatus, Long> taskStatusLongMap = service.countByStatus(null);
        assertTrue(taskStatusLongMap.isEmpty());
    }

    @Test
    void shouldAverageOnlyCompletedTasksWithScores() {
        List<AssessmentTask> tasks = List.of(task(1L, "一部", TaskStatus.COMPLETED, 80, LocalDateTime.of(2026, 8, 7, 9, 0)), task(2L, "一部", TaskStatus.COMPLETED, 100, LocalDateTime.of(2026, 8, 7, 10, 0)), task(3L, "一部", TaskStatus.PENDING, 60, LocalDateTime.of(2026, 8, 7, 11, 0)), task(4L, "二部", TaskStatus.COMPLETED, null, LocalDateTime.of(2026, 8, 7, 11, 0)), task(5L, "二部", TaskStatus.COMPLETED, 90, LocalDateTime.of(2026, 8, 7, 11, 0)));

        Map<String, Double> result = service.averageScoreByDepartment(tasks);

        assertEquals(90.0, result.get("一部"), 0.001);
        assertEquals(90.0, result.get("二部"), 0.001);

    }

    @Test
    void shouldSortByScoreThenCreatedAtDescending() {
        AssessmentTask score90Older = task(1L, "一部", TaskStatus.COMPLETED, 90, LocalDateTime.of(2026, 8, 7, 9, 0));
        AssessmentTask score100 = task(2L, "一部", TaskStatus.COMPLETED, 100, LocalDateTime.of(2026, 8, 7, 8, 0));
        AssessmentTask score90Newer = task(3L, "二部", TaskStatus.COMPLETED, 90, LocalDateTime.of(2026, 8, 7, 11, 0));
        AssessmentTask noScore = task(4L, "二部", TaskStatus.PENDING, null, LocalDateTime.of(2026, 8, 7, 12, 0));

        List<AssessmentTask> result = service.findTopScoredTasks(List.of(score90Older, score100, score90Newer, noScore), 10);

        List<Long> resultIds = result.stream().map(AssessmentTask::getId).toList();

        assertEquals(List.of(2L, 3L, 1L), resultIds);
    }

    @Test
    void limitShouldTruncateResult() {
        List<AssessmentTask> tasks = List.of(task(1L, "一部", TaskStatus.COMPLETED, 80, LocalDateTime.of(2026, 8, 7, 9, 0)), task(2L, "一部", TaskStatus.COMPLETED, 100, LocalDateTime.of(2026, 8, 7, 10, 0)), task(3L, "一部", TaskStatus.COMPLETED, 60, LocalDateTime.of(2026, 8, 7, 11, 0)));
        List<AssessmentTask> result = service.findTopScoredTasks(tasks, 2);
        List<Long> resultIds = result.stream().map(AssessmentTask::getId).toList();

        assertEquals(2, result.size());
        assertEquals(List.of(2L, 1L), resultIds);

    }

    @Test
    void nonPositiveLimitShouldReturnEmptyList() {
        List<AssessmentTask> tasks = List.of(task(1L, "一部", TaskStatus.COMPLETED, 80, LocalDateTime.of(2026, 8, 7, 9, 0)), task(2L, "一部", TaskStatus.COMPLETED, 100, LocalDateTime.of(2026, 8, 7, 10, 0)), task(3L, "一部", TaskStatus.COMPLETED, 60, LocalDateTime.of(2026, 8, 7, 11, 0)));
        List<AssessmentTask> result = service.findTopScoredTasks(tasks, -2);
        assertTrue(result.isEmpty());
        List<AssessmentTask> result2 = service.findTopScoredTasks(tasks, 0);
        assertTrue(result2.isEmpty());
    }

    @Test
    void topTasksShouldNotModifyOriginalList() {
        List<AssessmentTask> tasks = new ArrayList<>(List.of(task(1L, "一部", TaskStatus.COMPLETED, 80, LocalDateTime.of(2026, 8, 7, 9, 0)), task(2L, "一部", TaskStatus.COMPLETED, 100, LocalDateTime.of(2026, 8, 7, 10, 0)), task(3L, "一部", TaskStatus.COMPLETED, 60, LocalDateTime.of(2026, 8, 7, 11, 0))));

        List<AssessmentTask> original = new ArrayList<>(tasks);

        service.findTopScoredTasks(tasks, 2);

        assertEquals(original, tasks);
    }

    @Test
    void nullTasksShouldReturnEmptyStatistics() {
        assertTrue(service.averageScoreByDepartment(null).isEmpty());
        assertTrue(service.findTopScoredTasks(null, 3).isEmpty());
    }

    @Test
    void emptyTasksShouldReturnEmptyStatistics() {
        List<AssessmentTask> tasks = List.of();

        assertTrue(service.countByStatus(tasks).isEmpty());
        assertTrue(service.averageScoreByDepartment(tasks).isEmpty());
        assertTrue(service.findTopScoredTasks(tasks, 3).isEmpty());
    }
}