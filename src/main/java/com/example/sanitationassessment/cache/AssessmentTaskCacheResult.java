package com.example.sanitationassessment.cache;

import com.example.sanitationassessment.domain.AssessmentTask;

public record AssessmentTaskCacheResult(boolean hit, AssessmentTask task) {
    public static AssessmentTaskCacheResult miss() {
        return new AssessmentTaskCacheResult(false, null);
    }

    public static AssessmentTaskCacheResult hit(AssessmentTask task) {
        return new AssessmentTaskCacheResult(true, task);
    }
}
