package com.example.sanitationassessment.event;

import com.example.sanitationassessment.cache.AssessmentTaskCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssessmentTaskCacheEvictListenerTest {
    @Mock
    private AssessmentTaskCache assessmentTaskCache;

    @InjectMocks
    private AssessmentTaskCacheEvictListener listener;

    @Test
    void handleShouldEvictTaskCache() {
        AssessmentTaskUpdatedEvent event =
                new AssessmentTaskUpdatedEvent(1L);

        listener.handle(event);

        verify(assessmentTaskCache).evict(1L);
    }
}

