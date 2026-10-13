package com.argus.backend.model;

import java.util.List;

import com.argus.backend.dto.HistoricalAnalogue;
import com.argus.backend.enums.DivergenceType;
import com.argus.backend.enums.HistoricalCoverage;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DivergenceReport {

    /** Returned so a successful run is as traceable as a failed one, and so a client can
     *  later request this report by id. Errors already carry it via ErrorResponse. */
    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("agent_outputs")
    private List<AgentOutput> agentOutputs;

    @JsonProperty("divergence_type")
    private DivergenceType divergenceType;

    @JsonProperty("primary_thesis")
    private String primaryThesis;

    @JsonProperty("strongest_counter_thesis")
    private String strongestCounterThesis;

    @JsonProperty("key_conflicts")
    private List<Object> keyConflicts;

    @JsonProperty("historical_analogues")
    private List<HistoricalAnalogue> historicalAnalogues;

    @JsonProperty("historical_coverage")
    private HistoricalCoverage historicalCoverage;

    private List<String> tags;
}