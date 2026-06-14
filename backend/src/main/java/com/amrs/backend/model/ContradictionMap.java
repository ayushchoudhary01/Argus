package com.amrs.backend.model;

import com.amrs.backend.dto.DirectionalConflict;
import com.amrs.backend.dto.FlaggedTension;
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

    private List<DirectionalConflict> directionalConflicts;
    private List<FlaggedTension> flaggedTensions;
    private List<Object> entailments;
    private List<Object> neutral_pairs;
}