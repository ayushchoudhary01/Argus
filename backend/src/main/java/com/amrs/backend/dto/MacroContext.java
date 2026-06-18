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
public class MacroContext {

    @JsonProperty("yield_curve_slope")
    private Double yieldCurveSlope;

    @JsonProperty("dxy_momentum")
    private String dxyMomentum;
}
