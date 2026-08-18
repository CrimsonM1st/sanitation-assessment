package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.cache.AssessmentTaskCache;
import com.example.sanitationassessment.cache.AssessmentTaskCacheResult;
import com.example.sanitationassessment.lock.RedisLock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentTaskControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AssessmentTaskCache assessmentTaskCache;
    @MockitoBean
    private RedisLock redisLock;

    @BeforeEach
    void setUpCacheMiss() {
        when(assessmentTaskCache.get(anyLong()))
                .thenReturn(AssessmentTaskCacheResult.miss());
        when(redisLock.tryLock(
                anyString(),
                any(Duration.class)
        )).thenReturn("test-token");
    }

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
        long id = createTaskAndReturnId("环卫二部", "PROCESSING", null);
        mockMvc.perform(get("/assessment-tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
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

    @Test
    void updateStatusShouldBeSuccess() throws Exception {
        long id = createTaskAndReturnId("环卫一部", "PENDING", null);
        String updateRequestBody = """
                {
                  "status": "PROCESSING",
                  "score": null
                }
                """;
        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.score").doesNotExist());

        mockMvc.perform(get("/assessment-tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.score").doesNotExist());
    }

    @Test
    void updateStatusShouldReturn404WhenTaskDoesNotExist() throws Exception {
        String requestBody = """
                {
                  "status": "PROCESSING",
                  "score": null
                }
                """;

        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", 999999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("未查询到该任务"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void updateStatusShouldRejectNullStatus() throws Exception {
        String requestBody = """
                {
                  "status": null,
                  "score": null
                }
                """;

        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("任务状态不能为空"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void updateStatusShouldRejectScoreOver100() throws Exception {
        String requestBody = """
                {
                  "status": "COMPLETED",
                  "score": 101
                }
                """;

        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("分数不能高于100"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void updateStatusToCompletedShouldRejectNullScore() throws Exception {
        long id = createTaskAndReturnId("环卫一部", "PENDING", null);
        String updateRequestBody = """
                {
                  "status": "COMPLETED",
                  "score": null
                }
                """;
        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("已完成考评任务必须提供分数"));
    }


    @Test
    void updateStatusToProcessingShouldRejectNonNullScore() throws Exception {
        long id = createTaskAndReturnId("环卫一部", "PENDING", null);
        String updateRequestBody = """
                {
                  "status": "PROCESSING",
                  "score": 80
                }
                """;
        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("待完成考评任务和处理中考评任务不需要提供分数"));
    }

    @Test
    void updateStatusFromProcessingToCompletedScore90ShouldBeSuccess() throws Exception {
        long id = createTaskAndReturnId("环卫一部", "PROCESSING", null);
        String updateRequestBody = """
                {
                  "status": "COMPLETED",
                  "score": 90
                }
                """;
        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));


        mockMvc.perform(get("/assessment-tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.score").value(90));
    }

    @Test
    void updateStatusFromCompletedToProcessingShouldBeRejected() throws Exception {
        long id = createTaskAndReturnId("环卫一部", "COMPLETED", 90);

        String updateRequestBody = """
                {
                  "status": "PROCESSING",
                  "score": 90
                }
                """;
        mockMvc.perform(
                        put("/assessment-tasks/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));


        mockMvc.perform(get("/assessment-tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.score").value(90));
    }

    private long createTaskAndReturnId(
            String departmentName,
            String status,
            Integer score) throws Exception {
        String scoreJson = score == null
                ? "null"
                : score.toString();
        String createRequestBody = """
                {
                  "departmentName": "%s",
                  "status": "%s",
                  "score": %s
                }
                """.formatted(departmentName, status, scoreJson);

        MvcResult createResult = mockMvc.perform(
                        post("/assessment-tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody =
                createResult.getResponse().getContentAsString();

        Number id = JsonPath.read(responseBody, "$.data.id");
        return id.longValue();
    }

    @Test
    void cacheRebuildBusyShouldReturnServiceUnavailable()
            throws Exception {
        when(assessmentTaskCache.get(1L))
                .thenReturn(AssessmentTaskCacheResult.miss());

        when(redisLock.tryLock(
                anyString(),
                any(Duration.class)
        )).thenReturn(null);

        mockMvc.perform(get("/assessment-tasks/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.msg")
                        .value("缓存正在重建，请稍后重试"));
    }
}
