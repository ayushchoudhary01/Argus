package com.amrs.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank
    @Size(min = 10, max = 2000)
    private String event;

    @NotEmpty
    @JsonProperty("asset_context")
    private List<String> assetContext;

    private Instant timestamp;
}