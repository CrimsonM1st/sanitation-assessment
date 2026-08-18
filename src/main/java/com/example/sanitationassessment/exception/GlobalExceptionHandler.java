package com.example.sanitationassessment.exception;

import com.example.sanitationassessment.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest()
                .body(Result.error(400, message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.badRequest().body(Result.error(400, exception.getMessage()));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Result<Void>> handleTaskNotFoundException(TaskNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(404, exception.getMessage()));
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    public ResponseEntity<Result<Void>> handleConcurrentUpdateException(ConcurrentUpdateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.error(409, exception.getMessage()));
    }

    @ExceptionHandler(CacheRebuildBusyException.class)
    public ResponseEntity<Result<Void>> handleCacheRebuildBusyException(CacheRebuildBusyException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Result.error(503, exception.getMessage()));
    }
}
