package com.amrs.backend.service;

import com.amrs.backend.entity.DivergenceReportEntity;
import com.amrs.backend.model.DivergenceReport;
import com.amrs.backend.repository.DivergenceReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersistenceService {

    private final DivergenceReportRepository repository;
    private final ObjectMapper objectMapper;

    public void persist(DivergenceReport report, String correlationId) {
        try {
            String json = objectMapper.writeValueAsString(report);
            DivergenceReportEntity entity = DivergenceReportEntity.builder()
                    .correlationId(correlationId)
                    .divergenceType(report.getDivergenceType().name())
                    .reportJson(json)
                    .createdAt(Instant.now())
                    .build();
            repository.save(entity);
            log.info("Report persisted [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("Failed to persist report [correlationId={}]", correlationId, e);
        }
    }
}
