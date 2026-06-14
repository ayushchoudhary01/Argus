package com.amrs.backend.dto;

import com.amrs.backend.enums.CausalDirection;
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
    private String targetVariable;
    private String agent;
}
