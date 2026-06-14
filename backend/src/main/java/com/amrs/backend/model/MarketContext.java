package com.amrs.backend.model;

import com.amrs.backend.dto.HistoricalAnalogue;
import com.amrs.backend.dto.MacroContext;
import com.amrs.backend.dto.TechnicalContext;
import com.amrs.backend.dto.VolatilityContext;
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
public class MarketContext {

    private VolatilityContext volatility;
    private TechnicalContext technical;
    private MacroContext macro;
    private List<String> anomalyFlags;
    private HistoricalCoverage historicalCoverage;
    private List<HistoricalAnalogue> historicalSimilarity;
}