package com.amrs.backend.model;

import com.amrs.backend.dto.Assumption;
import com.amrs.backend.dto.CausalChain;
import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.enums.DivergenceAssessment;
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
public class AgentOutput {

    private String agent;
    private String thesis;

    @JsonProperty("reasoning_chain")
    private List<String> reasoningChain;

    @JsonProperty("causal_chain")
    private CausalChain causalChain;

    @JsonProperty("explicit_assumptions")
    private List<Assumption> explicitAssumptions;

    @JsonProperty("historical_refs")
    private List<String> historicalRefs;

    @JsonProperty("key_uncertainties")
    private List<String> keyUncertainties;

    private AgentStatus status;

    @JsonProperty("divergence_assessment")
    private DivergenceAssessment divergenceAssessment;

    @JsonProperty("challenged_assumptions")
    private List<String> challengedAssumptions;

    @JsonProperty("validated_assumptions")
    private List<String> validatedAssumptions;
}