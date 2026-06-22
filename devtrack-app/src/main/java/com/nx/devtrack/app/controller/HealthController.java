package com.nx.devtrack.app.controller;

import com.nx.devtrack.common.web.CommonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/devtrack/health")
public class HealthController {

    @GetMapping
    public CommonResponse<Map<String, String>> health() {
        return CommonResponse.ok(Map.of("status", "UP", "service", "devtrack"));
    }
}
