package com.example.sanitationassessment.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AssessmentTaskTest {

    @Test
    void sameNonNullIdShouldBeEqual() {
        // Arrange：准备两个ID相同、其他字段不同的任务
        AssessmentTask first = new AssessmentTask(
                1L,
                "环卫一部",
                TaskStatus.PENDING,
                null,
                LocalDateTime.of(2026, 8, 7, 9, 0)
        );

        AssessmentTask second = new AssessmentTask(
                1L,
                "环卫二部",
                TaskStatus.COMPLETED,
                95,
                LocalDateTime.of(2026, 8, 7, 10, 0)
        );

        // Act + Assert：验证业务身份只由ID决定
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void differentObjectsWithNullIdShouldNotBeEqual() {
        AssessmentTask first = new AssessmentTask();
        AssessmentTask second = new AssessmentTask();

        assertNotEquals(first, second);
    }

    @Test
    void objectShouldBeEqualToItself() {
        AssessmentTask task = new AssessmentTask();

        assertEquals(task, task);
    }

    @Test
    void sameIdShouldKeepOnlyOneTaskInHashSet() {
        AssessmentTask first = new AssessmentTask(
                1L,
                "环卫一部",
                TaskStatus.PENDING,
                null,
                LocalDateTime.of(2026, 8, 7, 9, 0)
        );
        AssessmentTask second = new AssessmentTask(
                1L,
                "环卫二部",
                TaskStatus.COMPLETED,
                95,
                LocalDateTime.of(2026, 8, 7, 10, 0)
        );

        Set<AssessmentTask> tasks = new HashSet<>();
        tasks.add(first);
        tasks.add(second);

        assertEquals(1, tasks.size());
    }
}
