package com.example.sanitationassessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("assessment_task_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentTaskAuditLogEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("task_id")
    private Long taskId;
    @TableField("action")
    private String action;
    @TableField("detail")
    private String detail;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
