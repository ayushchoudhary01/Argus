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
public class VolatilityContext {

    @JsonProperty("vix_level")
    private Double vixLevel;

    @JsonProperty("percentile_30d")
    private Double percentile30d;
}
