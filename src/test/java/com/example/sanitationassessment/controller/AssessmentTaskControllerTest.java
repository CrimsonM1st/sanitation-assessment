package com.example.sanitationassessment.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentTaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAssessmentTask() throws Exception {
        String requestBody = """
                {
                  "departmentName": "环卫一部",
                  "status": "COMPLETED",
                  "score": 90
                }
                """;

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.departmentName").value("环卫一部"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.score").value(90))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void shouldDepartmentNameNotEmpty() throws Exception {
        String requestBody = """
                {
                   "departmentName": " ",
                   "status": "PENDING",
                   "score": null
                }
                """;

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("部门名称不能为空"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldScoreMax100() throws Exception {
        String requestBody = """
                {
                   "departmentName": "环卫一部",
                   "status": "COMPLETED",
                   "score": 101
                }
                """;

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("分数不能高于100"));
    }

    @Test
    void shouldCompletedHaveScore() throws Exception {
        String requestBody = """
                {
                  "departmentName": "环卫一部",
                  "status": "COMPLETED",
                  "score": null
                }
                """;

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("已完成任务必须提供分数"));
    }

    @Test
    void shouldFindCreatedTaskById() throws Exception {
        String requestBody = """
                {
                  "departmentName": "环卫二部",
                  "status": "PROCESSING",
                  "score": null
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/assessment-tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody =
                createResult.getResponse().getContentAsString();

        Number id = JsonPath.read(responseBody, "$.data.id");

        mockMvc.perform(get("/assessment-tasks/{id}", id.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id.longValue()))
                .andExpect(jsonPath("$.data.departmentName").value("环卫二部"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void missingTaskShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/assessment-tasks/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("考评任务不存在，id=999999"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void scoreBelowZeroShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "departmentName": "环卫一部",
                  "status": "COMPLETED",
                  "score": -1
                }
                """;

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("分数不能低于0"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void queryShouldReturnDefaultPage() throws Exception {
        mockMvc.perform(get("/assessment-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pages").isNumber());
    }

    @Test
    void queryShouldFilterByDepartmentNameAndStatus() throws Exception {
        String departmentName = "测试部门-" + System.nanoTime();

        String completedTask = """
                {
                  "departmentName": "%s",
                  "status": "COMPLETED",
                  "score": 90
                }
                """.formatted(departmentName);

        String pendingTask = """
                {
                  "departmentName": "%s",
                  "status": "PENDING"
                }
                """.formatted(departmentName);

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completedTask))
                .andExpect(status().isOk());

        mockMvc.perform(post("/assessment-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pendingTask))
                .andExpect(status().isOk());

        mockMvc.perform(get("/assessment-tasks")
                        .param("departmentName", departmentName)
                        .param("status", "COMPLETED")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].departmentName")
                        .value(departmentName))
                .andExpect(jsonPath("$.data.records[0].status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data.records[0].score").value(90));
    }

    @Test
    void queryShouldRejectInvalidPageNum() throws Exception {
        mockMvc.perform(get("/assessment-tasks")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void queryShouldRejectOversizedPageSize() throws Exception {
        mockMvc.perform(get("/assessment-tasks")
                        .param("pageNum", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
