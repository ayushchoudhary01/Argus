package com.amrs.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalContext {

    @JsonProperty("spy_rsi_14")
    private Double spyRsi14;

    @JsonProperty("spy_trend")
    private String spyTrend;
}
