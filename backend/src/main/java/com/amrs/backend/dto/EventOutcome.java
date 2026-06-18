package com.amrs.backend.dto;

import com.amrs.backend.enums.DataQuality;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOutcome {

    private String ticker;

    @JsonProperty("window_days")
    private Integer windowDays;

    @JsonProperty("return_pct")
    private Double returnPct;

    private DataQuality quality;
}
