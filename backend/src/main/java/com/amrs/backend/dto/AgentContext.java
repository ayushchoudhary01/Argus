package com.amrs.backend.dto;

import com.amrs.backend.model.MarketContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    private MarketContext marketContext;
    private String agent1Thesis;
    private List<String> agent1ReasoningChain;
    private CausalChain agent1CausalChain;
    private List<Assumption> agent1Assumptions;
    private String agent2Thesis;
    private List<String> agent2ReasoningChain;
    private CausalChain agent2CausalChain;
    private List<Assumption> agent2Assumptions;
}