package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.auth.AuthenticatedUser;
import com.example.sanitationassessment.common.Result;
import com.example.sanitationassessment.dto.auth.LoginRequest;
import com.example.sanitationassessment.dto.auth.LoginResponse;
import com.example.sanitationassessment.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authenticationService.authenticate(request));
    }

    @GetMapping("/me")
    public Result<AuthenticatedUser> currentUser(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser) {

        return Result.success(authenticatedUser);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        authenticationService.logout(authenticatedUser);

        return Result.success(
                "退出成功",
                null
        );
    }
}
