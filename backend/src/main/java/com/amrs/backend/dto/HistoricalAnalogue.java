package com.amrs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalAnalogue {

    private String eventId;
    private String title;
    private String date;
    private Double similarityScore;
    private String whatHappened;
    private List<EventOutcome> outcomes;
}
