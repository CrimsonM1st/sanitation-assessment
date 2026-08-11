package com.example.sanitationassessment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.sanitationassessment.domain.TaskStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("assessment_task")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentTaskEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("department_name")
    private String departmentName;
    @TableField("status")
    private TaskStatus status;
    @TableField("score")
    private Integer score;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @Version
    private Integer version;
}
