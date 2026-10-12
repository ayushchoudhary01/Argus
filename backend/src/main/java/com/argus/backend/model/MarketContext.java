package com.argus.backend.model;

import com.argus.backend.dto.HistoricalAnalogue;
import com.argus.backend.dto.MacroContext;
import com.argus.backend.dto.TechnicalContext;
import com.argus.backend.dto.VolatilityContext;
import com.argus.backend.enums.HistoricalCoverage;
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