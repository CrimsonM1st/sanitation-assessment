package com.example.sanitationassessment.mapper;

import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.entity.AssessmentTaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AssessmentTaskOptimisticLockTest {
    @Autowired
    private AssessmentTaskMapper mapper;

    @Test
    void oldVersionShouldNotOverwriteNewVersion() {
        AssessmentTaskEntity assessmentTaskEntity = new AssessmentTaskEntity();
        assessmentTaskEntity.setStatus(TaskStatus.PROCESSING);
        assessmentTaskEntity.setDepartmentName("乐观锁测试-" + System.nanoTime());
        LocalDateTime now = LocalDateTime.now();
        assessmentTaskEntity.setCreatedAt(now);
        assessmentTaskEntity.setUpdatedAt(now);
        assertEquals(1, mapper.insert(assessmentTaskEntity));
        Long id = assessmentTaskEntity.getId();
        AssessmentTaskEntity assessmentTaskEntityA = mapper.selectById(id);
        AssessmentTaskEntity assessmentTaskEntityB = mapper.selectById(id);
        assertEquals(0, assessmentTaskEntityA.getVersion());
        assertEquals(0, assessmentTaskEntityB.getVersion());
        assessmentTaskEntityA.setStatus(TaskStatus.COMPLETED);
        assessmentTaskEntityA.setScore(90);
        assertEquals(1, mapper.updateById(assessmentTaskEntityA));
        assessmentTaskEntityB.setStatus(TaskStatus.COMPLETED);
        assessmentTaskEntityB.setScore(80);
        assertEquals(0, mapper.updateById(assessmentTaskEntityB));
        AssessmentTaskEntity assessmentTaskEntityNew = mapper.selectById(id);
        assertEquals(90, assessmentTaskEntityNew.getScore());
        assertEquals(1, assessmentTaskEntityNew.getVersion());

    }
}
