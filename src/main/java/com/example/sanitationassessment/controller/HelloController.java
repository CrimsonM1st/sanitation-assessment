package com.example.sanitationassessment.controller;

import com.example.sanitationassessment.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello Sanitation");
    }
}
