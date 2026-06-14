package com.amrs.backend.model;

import com.amrs.backend.dto.HistoricalAnalogue;
import com.amrs.backend.enums.DivergenceType;
import com.amrs.backend.enums.HistoricalCoverage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DivergenceReport {

    private DivergenceType divergenceType;
    private String primaryThesis;
    private String strongestCounterThesis;
    private List<Object> keyConflicts;
    private List<HistoricalAnalogue> historicalAnalogues;
    private HistoricalCoverage historicalCoverage;
    private List<String> tags;
}