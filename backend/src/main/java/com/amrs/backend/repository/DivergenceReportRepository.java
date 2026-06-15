package com.amrs.backend.repository;

import com.amrs.backend.entity.DivergenceReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DivergenceReportRepository extends JpaRepository<DivergenceReportEntity, Long> {

    Optional<DivergenceReportEntity> findByCorrelationId(String correlationId);
}
