package com.amrs.backend.service;

import com.amrs.backend.model.EventRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrchestrationServiceCacheKeyTest {

    // All collaborators are null because buildCacheKey is a pure function that
    // touches none of them. This avoids standing up Spring context for a string
    // hashing test.
    private final OrchestrationService orchestrationService =
            new OrchestrationService(null, null, null, null, null, null);

    @Test
    void identicalEventAndAssets_produceIdenticalCacheKey() {
        EventRequest first = EventRequest.builder()
                .event("US CPI prints 3.8% vs 3.4% forecast.")
                .assetContext(List.of("SPY", "TLT", "GLD"))
                .timestamp(Instant.parse("2026-06-18T12:30:00Z"))
                .build();

        EventRequest second = EventRequest.builder()
                .event("US CPI prints 3.8% vs 3.4% forecast.")
                .assetContext(List.of("SPY", "TLT", "GLD"))
                .timestamp(Instant.parse("2026-06-19T08:00:00Z")) // different timestamp
                .build();

        assertThat(orchestrationService.buildCacheKey(first))
                .isEqualTo(orchestrationService.buildCacheKey(second));
    }

    @Test
    void differentTimestamps_doNotAffectCacheKey() {
        // This is a regression test for the bug where timestamp was originally part
        // of the cache key, making every request unique and defeating caching entirely.
        EventRequest now = EventRequest.builder()
                .event("Fed cuts rates 50bps in emergency move.")
                .assetContext(List.of("SPY"))
                .timestamp(Instant.now())
                .build();

        EventRequest later = EventRequest.builder()
                .event("Fed cuts rates 50bps in emergency move.")
                .assetContext(List.of("SPY"))
                .timestamp(Instant.now().plusSeconds(3600))
                .build();

        assertThat(orchestrationService.buildCacheKey(now))
                .isEqualTo(orchestrationService.buildCacheKey(later));
    }

    @Test
    void caseAndWhitespaceDifferences_produceIdenticalCacheKey() {
        EventRequest first = EventRequest.builder()
                .event("CPI rises 3.8%")
                .assetContext(List.of("SPY", "TLT"))
                .timestamp(Instant.now())
                .build();

        EventRequest second = EventRequest.builder()
                .event("  cpi   rises 3.8%  ")
                .assetContext(List.of("SPY", "TLT"))
                .timestamp(Instant.now())
                .build();

        assertThat(orchestrationService.buildCacheKey(first))
                .isEqualTo(orchestrationService.buildCacheKey(second));
    }

    @Test
    void assetOrderDoesNotAffectCacheKey() {
        EventRequest first = EventRequest.builder()
                .event("Oil prices surge 15% on supply shock.")
                .assetContext(List.of("SPY", "TLT", "GLD"))
                .timestamp(Instant.now())
                .build();

        EventRequest second = EventRequest.builder()
                .event("Oil prices surge 15% on supply shock.")
                .assetContext(List.of("GLD", "SPY", "TLT")) // reordered
                .timestamp(Instant.now())
                .build();

        assertThat(orchestrationService.buildCacheKey(first))
                .isEqualTo(orchestrationService.buildCacheKey(second));
    }

    @Test
    void differentEventText_producesDifferentCacheKey() {
        EventRequest first = EventRequest.builder()
                .event("US CPI prints 3.8% vs 3.4% forecast.")
                .assetContext(List.of("SPY"))
                .timestamp(Instant.now())
                .build();

        EventRequest second = EventRequest.builder()
                .event("US CPI prints 4.1% vs 3.4% forecast.")
                .assetContext(List.of("SPY"))
                .timestamp(Instant.now())
                .build();

        assertThat(orchestrationService.buildCacheKey(first))
                .isNotEqualTo(orchestrationService.buildCacheKey(second));
    }

    @Test
    void differentAssetSet_producesDifferentCacheKey() {
        EventRequest first = EventRequest.builder()
                .event("Same event text here.")
                .assetContext(List.of("SPY"))
                .timestamp(Instant.now())
                .build();

        EventRequest second = EventRequest.builder()
                .event("Same event text here.")
                .assetContext(List.of("SPY", "TLT"))
                .timestamp(Instant.now())
                .build();

        assertThat(orchestrationService.buildCacheKey(first))
                .isNotEqualTo(orchestrationService.buildCacheKey(second));
    }

    @Test
    void cacheKey_alwaysStartsWithExpectedPrefix() {
        EventRequest request = EventRequest.builder()
                .event("Any event text at all.")
                .assetContext(List.of("SPY"))
                .timestamp(Instant.now())
                .build();

        assertThat(orchestrationService.buildCacheKey(request)).startsWith("amrs:analysis:");
    }
}
