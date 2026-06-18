package com.amrs.backend.model;

import com.amrs.backend.dto.DirectionalConflict;
import com.amrs.backend.dto.FlaggedTension;
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
public class ContradictionMap {

    @JsonProperty("directional_conflicts")
    private List<DirectionalConflict> directionalConflicts;

    @JsonProperty("flagged_tensions")
    private List<FlaggedTension> flaggedTensions;

    private List<Object> entailments;

    @JsonProperty("neutral_pairs")
    private List<Object> neutralPairs;
}