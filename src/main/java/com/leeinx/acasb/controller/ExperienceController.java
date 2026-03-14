package com.leeinx.acasb.controller;

import com.leeinx.acasb.dto.ApiResponse;
import com.leeinx.acasb.dto.ExperienceValidateRequest;
import com.leeinx.acasb.service.ExperienceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/experience")
@CrossOrigin(origins = "*")
public class ExperienceController {
    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping("/rank-rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRankRules(@RequestParam String rankId) {
        return ResponseEntity.ok(ApiResponse.success(experienceService.getRankRules(rankId)));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validate(
            @RequestBody ExperienceValidateRequest request,
            @RequestParam(value = "enable_ai", required = false) Boolean enableAi) {
        return ResponseEntity.ok(ApiResponse.success(experienceService.validate(request, enableAi)));
    }
}
