package com.amrs.backend.dto;

import com.amrs.backend.enums.DataQuality;
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
    private Integer windowDays;
    private Double returnPct;
    private DataQuality quality;
}
