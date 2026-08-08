package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.common.Result;
import com.example.sanitationassessment.domain.AssessmentTask;
import com.example.sanitationassessment.dto.assessment.CreateAssessmentTaskRequest;
import com.example.sanitationassessment.service.AssessmentTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assessment-tasks")
public class AssessmentTaskController {
    private final AssessmentTaskService assessmentTaskService;

    public AssessmentTaskController(AssessmentTaskService assessmentTaskService) {
        this.assessmentTaskService = assessmentTaskService;
    }

    @PostMapping
    public Result<AssessmentTask> create(@Valid @RequestBody CreateAssessmentTaskRequest request) {
        AssessmentTask assessmentTask = assessmentTaskService.create(request);
        return Result.success(assessmentTask);
    }

    @GetMapping("/{id}")
    public Result<AssessmentTask> findById(@PathVariable("id") Long id) {
        AssessmentTask assessmentTask = assessmentTaskService.findById(id);
        return Result.success(assessmentTask);

    }
}
