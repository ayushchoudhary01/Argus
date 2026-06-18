package com.amrs.backend.model;

import com.amrs.backend.dto.HistoricalAnalogue;
import com.amrs.backend.dto.MacroContext;
import com.amrs.backend.dto.TechnicalContext;
import com.amrs.backend.dto.VolatilityContext;
import com.amrs.backend.enums.HistoricalCoverage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketContext {

    private VolatilityContext volatility;
    private TechnicalContext technical;
    private MacroContext macro;

    @JsonProperty("anomaly_flags")
    private List<String> anomalyFlags;

    @JsonProperty("historical_coverage")
    private HistoricalCoverage historicalCoverage;

    @JsonProperty("historical_similarity")
    private List<HistoricalAnalogue> historicalSimilarity;
}