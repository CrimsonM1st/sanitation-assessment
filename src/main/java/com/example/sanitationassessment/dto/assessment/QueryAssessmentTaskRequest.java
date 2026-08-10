package com.example.sanitationassessment.dto.assessment;

import com.example.sanitationassessment.domain.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueryAssessmentTaskRequest {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量最小为 1")
    @Max(value = 100, message = "每页数量最大为 100")
    private Integer pageSize = 10;
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String departmentName;
    private TaskStatus status;

}
