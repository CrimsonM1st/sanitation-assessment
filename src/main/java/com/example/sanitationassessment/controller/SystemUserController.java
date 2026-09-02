package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.common.Result;
import com.example.sanitationassessment.dto.user.CreateSystemUserRequest;
import com.example.sanitationassessment.dto.user.QuerySystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.service.SystemUserService;
import com.example.sanitationassessment.vo.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class SystemUserController {

    private final SystemUserService systemUserService;

    public SystemUserController(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SystemUserResponse> create(
            @Valid @RequestBody CreateSystemUserRequest request) {
        return Result.success(systemUserService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<SystemUserResponse>> query(@Valid @ModelAttribute QuerySystemUserRequest request) {
        return Result.success(systemUserService.query(request));
    }
}
