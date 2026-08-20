package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.common.Result;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.service.SystemUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class SystemUserController {

    private final SystemUserService systemUserService;

    public SystemUserController(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    @PostMapping
    public Result<SystemUserResponse> create(
            @Valid @RequestBody CreateSystemUserRequest request) {
        return Result.success(systemUserService.create(request));
    }
}