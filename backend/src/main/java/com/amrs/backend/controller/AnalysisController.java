package com.amrs.backend.controller;

import com.amrs.backend.model.DivergenceReport;
import com.amrs.backend.model.EventRequest;
import com.amrs.backend.service.OrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Validated
public class AnalysisController {

    private final OrchestrationService orchestrationService;

    @PostMapping
    public ResponseEntity<DivergenceReport> analyse(@RequestBody @Valid EventRequest request) {
        return ResponseEntity.ok(orchestrationService.analyse(request));
    }
}