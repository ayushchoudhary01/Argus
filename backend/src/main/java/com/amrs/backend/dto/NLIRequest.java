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
public class NLIRequest {

    private List<Assumption> assumptions;
    private List<CausalChain> causalChains;
}
