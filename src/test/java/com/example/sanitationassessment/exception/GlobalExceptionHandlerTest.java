package com.example.sanitationassessment.exception;

import com.example.sanitationassessment.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    @Test
    void concurrentUpdateExceptionShouldReturn409() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        ConcurrentUpdateException concurrentUpdateException = new ConcurrentUpdateException(
                "任务已被其他请求修改，请刷新后重试");
        ResponseEntity<Result<Void>> resultResponseEntity = globalExceptionHandler.handleConcurrentUpdateException(
                concurrentUpdateException);
        assertEquals(HttpStatus.CONFLICT, resultResponseEntity.getStatusCode());
        assertNotNull(resultResponseEntity.getBody());
        assertEquals(409, resultResponseEntity.getBody().getCode());
        assertEquals("任务已被其他请求修改，请刷新后重试", resultResponseEntity.getBody().getMsg());
    }

}
