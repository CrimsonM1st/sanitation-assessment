package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.domain.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssessmentStatisticsService {
    public Map<TaskStatus, Long> countByStatus(List<AssessmentTask> tasks) {
        List<AssessmentTask> safeTasks =
                tasks == null ? Collections.emptyList() : tasks;
        return safeTasks.stream().collect(Collectors.groupingBy(e -> e.getStatus(),
                Collectors.counting()
        ));
    }

    public Map<String, Double> averageScoreByDepartment(List<AssessmentTask> tasks) {
        List<AssessmentTask> safeTasks =
                tasks == null ? Collections.emptyList() : tasks;
        return safeTasks.stream().filter(e -> TaskStatus.COMPLETED.equals(e.getStatus()))
                .filter(e -> e.getScore() != null)
                .collect(Collectors.groupingBy(AssessmentTask::getDepartmentName, Collectors.averagingInt(AssessmentTask::getScore)));
    }

    public List<AssessmentTask> findTopScoredTasks(List<AssessmentTask> tasks, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<AssessmentTask> safeTasks =
                tasks == null ? Collections.emptyList() : tasks;
        Comparator<AssessmentTask> assessmentTaskComparator = Comparator.comparing(
                AssessmentTask::getScore,
                Comparator.reverseOrder()
        ).thenComparing(
                AssessmentTask::getCreatedAt,
                Comparator.reverseOrder()
        );
        return safeTasks.stream()
                .filter(task -> task.getScore() != null)
                .sorted(assessmentTaskComparator)
                .limit(limit)
                .toList();
    }
}
