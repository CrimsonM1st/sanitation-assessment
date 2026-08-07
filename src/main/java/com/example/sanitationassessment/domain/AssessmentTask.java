package com.example.sanitationassessment.domain;


import java.time.LocalDateTime;
import java.util.Objects;

public class AssessmentTask {
    private Long id;
    private String departmentName;
    private TaskStatus status;
    private Integer score;
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) return false;
        AssessmentTask that = (AssessmentTask) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public AssessmentTask() {
    }

    public AssessmentTask(Long id, String departmentName, TaskStatus status, Integer score, LocalDateTime createdAt) {
        this.id = id;
        this.departmentName = departmentName;
        this.status = status;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
