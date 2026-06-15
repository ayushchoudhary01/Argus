package com.amrs.backend.model;

import com.amrs.backend.dto.Assumption;
import com.amrs.backend.dto.CausalChain;
import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.enums.DivergenceAssessment;
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
    private List<String> reasoningChain;
    private CausalChain causalChain;
    private List<Assumption> explicitAssumptions;
    private List<String> historicalRefs;
    private List<String> keyUncertainties;
    private AgentStatus status;
    private DivergenceAssessment divergenceAssessment;
    private List<String> challengedAssumptions;
    private List<String> validatedAssumptions;
}