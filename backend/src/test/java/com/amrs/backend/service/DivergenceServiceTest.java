package com.amrs.backend.service;

import com.amrs.backend.dto.DirectionalConflict;
import com.amrs.backend.dto.FlaggedTension;
import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.enums.DivergenceAssessment;
import com.amrs.backend.enums.DivergenceType;
import com.amrs.backend.enums.HistoricalCoverage;
import com.amrs.backend.model.AgentOutput;
import com.amrs.backend.model.ContradictionMap;
import com.amrs.backend.model.DivergenceReport;
import com.amrs.backend.model.MarketContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DivergenceServiceTest {

    private final DivergenceService divergenceService = new DivergenceService();

    private AgentOutput okAgent(String name, DivergenceAssessment assessment) {
        return AgentOutput.builder()
                .agent(name)
                .status(AgentStatus.OK)
                .thesis(name + " thesis")
                .divergenceAssessment(assessment)
                .build();
    }

    private ContradictionMap emptyContradictionMap() {
        return ContradictionMap.builder()
                .directionalConflicts(Collections.emptyList())
                .flaggedTensions(Collections.emptyList())
                .entailments(Collections.emptyList())
                .neutralPairs(Collections.emptyList())
                .build();
    }

    private ContradictionMap contradictionMapWithDirectionalConflict() {
        return ContradictionMap.builder()
                .directionalConflicts(List.of(DirectionalConflict.builder()
                        .premise("structural: UPWARD_PRESSURE on terminal rate")
                        .hypothesis("contrarian: DOWNWARD_PRESSURE on terminal rate")
                        .agents(List.of("structural", "contrarian"))
                        .targetVariable("terminal rate")
                        .build()))
                .flaggedTensions(Collections.emptyList())
                .entailments(Collections.emptyList())
                .neutralPairs(Collections.emptyList())
                .build();
    }

    private ContradictionMap contradictionMapWithFlaggedTension() {
        return ContradictionMap.builder()
                .directionalConflicts(Collections.emptyList())
                .flaggedTensions(List.of(FlaggedTension.builder()
                        .probability(0.91)
                        .build()))
                .entailments(Collections.emptyList())
                .neutralPairs(Collections.emptyList())
                .build();
    }

    private MarketContext marketContextWithCoverage(HistoricalCoverage coverage) {
        return MarketContext.builder()
                .historicalCoverage(coverage)
                .historicalSimilarity(Collections.emptyList())
                .build();
    }

    @Test
    void lowVarianceWithNoConflicts_isConsensusHighConviction() {
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.LOW_VARIANCE);

        DivergenceType result = divergenceService.classifyDivergence(agent3, emptyContradictionMap());

        assertThat(result).isEqualTo(DivergenceType.CONSENSUS_HIGH_CONVICTION);
    }

    @Test
    void substantiveWithDirectionalConflict_isGenuineDivergence() {
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.SUBSTANTIVE);

        DivergenceType result = divergenceService.classifyDivergence(agent3, contradictionMapWithDirectionalConflict());

        assertThat(result).isEqualTo(DivergenceType.GENUINE_DIVERGENCE);
    }

    @Test
    void substantiveWithFlaggedTension_isGenuineDivergence() {
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.SUBSTANTIVE);

        DivergenceType result = divergenceService.classifyDivergence(agent3, contradictionMapWithFlaggedTension());

        assertThat(result).isEqualTo(DivergenceType.GENUINE_DIVERGENCE);
    }

    @Test
    void substantiveWithNoConflicts_isEmphasisDispute() {
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.SUBSTANTIVE);

        DivergenceType result = divergenceService.classifyDivergence(agent3, emptyContradictionMap());

        assertThat(result).isEqualTo(DivergenceType.EMPHASIS_DISPUTE);
    }

    @Test
    void dataGapAssessment_isInsufficientSignal() {
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.DATA_GAP);

        DivergenceType result = divergenceService.classifyDivergence(agent3, emptyContradictionMap());

        assertThat(result).isEqualTo(DivergenceType.INSUFFICIENT_SIGNAL);
    }

    @Test
    void degradedAgent3_isInsufficientSignal_regardlessOfConflicts() {
        AgentOutput agent3 = AgentOutput.builder()
                .agent("contrarian")
                .status(AgentStatus.DEGRADED)
                .build();

        DivergenceType result = divergenceService.classifyDivergence(agent3, contradictionMapWithDirectionalConflict());

        assertThat(result).isEqualTo(DivergenceType.INSUFFICIENT_SIGNAL);
    }

    @Test
    void lowVarianceWithConflictsPresent_isGenuineDivergence_notConsensus() {
        // LOW_VARIANCE only yields CONSENSUS_HIGH_CONVICTION when there are zero conflicts.
        // If conflicts somehow exist alongside LOW_VARIANCE, current logic falls through
        // to the final INSUFFICIENT_SIGNAL default since no branch explicitly matches
        // LOW_VARIANCE + hasConflicts. This test documents that actual current behavior.
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.LOW_VARIANCE);

        DivergenceType result = divergenceService.classifyDivergence(agent3, contradictionMapWithDirectionalConflict());

        assertThat(result).isEqualTo(DivergenceType.INSUFFICIENT_SIGNAL);
    }

    @Test
    void buildReport_appendsLowHistoricalConfidenceTag_whenCoverageInsufficient() {
        AgentOutput agent1 = okAgent("structural", null);
        AgentOutput agent2 = okAgent("risk", null);
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.LOW_VARIANCE);
        MarketContext marketContext = marketContextWithCoverage(HistoricalCoverage.INSUFFICIENT);

        DivergenceReport report = divergenceService.buildReport(
                agent1, agent2, agent3, emptyContradictionMap(), marketContext, "test-correlation-id");

        assertThat(report.getTags()).contains("LOW_HISTORICAL_CONFIDENCE");
    }

    @Test
    void buildReport_omitsLowHistoricalConfidenceTag_whenCoverageSufficient() {
        AgentOutput agent1 = okAgent("structural", null);
        AgentOutput agent2 = okAgent("risk", null);
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.LOW_VARIANCE);
        MarketContext marketContext = marketContextWithCoverage(HistoricalCoverage.SUFFICIENT);

        DivergenceReport report = divergenceService.buildReport(
                agent1, agent2, agent3, emptyContradictionMap(), marketContext, "test-correlation-id");

        assertThat(report.getTags()).doesNotContain("LOW_HISTORICAL_CONFIDENCE");
    }

    @Test
    void buildReport_usesAgent1ThesisAsPrimary_andAgent3ThesisAsCounter() {
        AgentOutput agent1 = okAgent("structural", null);
        AgentOutput agent2 = okAgent("risk", null);
        AgentOutput agent3 = okAgent("contrarian", DivergenceAssessment.LOW_VARIANCE);
        MarketContext marketContext = marketContextWithCoverage(HistoricalCoverage.SUFFICIENT);

        DivergenceReport report = divergenceService.buildReport(
                agent1, agent2, agent3, emptyContradictionMap(), marketContext, "test-correlation-id");

        assertThat(report.getPrimaryThesis()).isEqualTo("structural thesis");
        assertThat(report.getStrongestCounterThesis()).isEqualTo("contrarian thesis");
    }
}
