package com.example.aicodemother.controller;

import com.example.aicodemother.common.BaseResponse;
import com.example.aicodemother.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping
    public BaseResponse<String> heathCHeck() {
        return ResultUtils.success("OK");
    }
}
