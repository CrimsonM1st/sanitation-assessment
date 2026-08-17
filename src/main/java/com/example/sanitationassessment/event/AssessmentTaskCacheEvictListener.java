package com.example.sanitationassessment.event;

import com.example.sanitationassessment.cache.AssessmentTaskCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AssessmentTaskCacheEvictListener {
    private final AssessmentTaskCache assessmentTaskCache;

    public AssessmentTaskCacheEvictListener(AssessmentTaskCache assessmentTaskCache) {
        this.assessmentTaskCache = assessmentTaskCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AssessmentTaskChangedEvent event) {
        // 根据事件里的 taskId 删除缓存
        Long taskId = event.taskId();
        assessmentTaskCache.evict(taskId);
    }
}
