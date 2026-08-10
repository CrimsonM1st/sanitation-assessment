package com.example.sanitationassessment.dto.assessment;

import com.example.sanitationassessment.domain.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssessmentTaskStatusRequest {
    @NotNull(message = "任务状态不能为空")
    private TaskStatus status;
    @Min(value = 0, message = "分数不能低于0")
    @Max(value = 100, message = "分数不能高于100")
    private Integer score;
}
