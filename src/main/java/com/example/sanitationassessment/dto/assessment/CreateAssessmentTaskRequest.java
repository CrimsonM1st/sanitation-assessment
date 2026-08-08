package com.example.sanitationassessment.dto.assessment;

import com.example.sanitationassessment.domain.TaskStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssessmentTaskRequest {
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String departmentName;

    @NotNull(message = "任务状态不能为空")
    private TaskStatus status;

    @Min(value = 0, message = "分数不能低于0")
    @Max(value = 100, message = "分数不能高于100")
    private Integer score;

}
