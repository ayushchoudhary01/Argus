package com.argus.backend.dto;

import com.argus.backend.enums.CausalDirection;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CausalChain {

    private String driver;
    private CausalDirection direction;

    @JsonProperty("target_variable")
    private String targetVariable;

    private String agent;
}
