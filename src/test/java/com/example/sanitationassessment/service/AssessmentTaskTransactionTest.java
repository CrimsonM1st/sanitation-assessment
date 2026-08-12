package com.example.sanitationassessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.sanitationassessment.domain.TaskStatus;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.entity.AssessmentTaskAuditLogEntity;
import com.example.sanitationassessment.exception.BusinessException;
import com.example.sanitationassessment.mapper.AssessmentTaskAuditLogMapper;
import com.example.sanitationassessment.mapper.AssessmentTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AssessmentTaskTransactionTest {
    @MockitoBean
    private AssessmentTaskAuditLogMapper assessmentTaskAuditLogMapper;
    @Autowired
    private AssessmentTaskService assessmentTaskService;
    @Autowired
    private AssessmentTaskMapper assessmentTaskMapper;

    @Test
    void createShouldRollbackTaskWhenAuditLogInsertFails() {
        when(assessmentTaskAuditLogMapper.insert(any(AssessmentTaskAuditLogEntity.class)))
                .thenReturn(0);

        CreateAssessmentTaskRequest request =
                new CreateAssessmentTaskRequest();

        request.setStatus(TaskStatus.COMPLETED);
        request.setScore(90);
        request.setDepartmentName("一部");

        int before = assessmentTaskMapper.selectCount(Wrappers.emptyWrapper()).intValue();

        BusinessException businessException = assertThrows(BusinessException.class,
                () -> assessmentTaskService.create(request));
        assertEquals(
                "记录审计日志失败",
                businessException.getMessage()
        );


        int after = assessmentTaskMapper.selectCount(Wrappers.emptyWrapper()).intValue();
        assertEquals(before, after);

    }

}
