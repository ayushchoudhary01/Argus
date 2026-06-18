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
public class DirectionalConflict {

    private String premise;
    private String hypothesis;
    private String type;
    private String method;
    private List<String> agents;

    @JsonProperty("target_variable")
    private String targetVariable;
}
