package com.amrs.backend.dto;

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
public class HistoricalAnalogue {

    @JsonProperty("event_id")
    private String eventId;

    private String title;
    private String date;

    @JsonProperty("similarity_score")
    private Double similarityScore;

    @JsonProperty("what_happened")
    private String whatHappened;

    private List<EventOutcome> outcomes;
}
