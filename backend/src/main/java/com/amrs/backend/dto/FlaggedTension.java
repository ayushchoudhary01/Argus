package com.amrs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaggedTension {

    private Assumption premise;
    private Assumption hypothesis;
    private String label;
    private Double probability;
    private String method;
    private String note;
}
