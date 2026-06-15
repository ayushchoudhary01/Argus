package com.amrs.backend.service;

import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.enums.DivergenceAssessment;
import com.amrs.backend.enums.DivergenceType;
import com.amrs.backend.enums.HistoricalCoverage;
import com.amrs.backend.model.AgentOutput;
import com.amrs.backend.model.ContradictionMap;
import com.amrs.backend.model.DivergenceReport;
import com.amrs.backend.model.MarketContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DivergenceService {

    public DivergenceReport buildReport(
            AgentOutput agent1,
            AgentOutput agent2,
            AgentOutput agent3,
            ContradictionMap contradictionMap,
            MarketContext marketContext,
            String correlationId
    ) {
        DivergenceType divergenceType = classifyDivergence(agent3, contradictionMap);

        log.info("Divergence classified as {} [correlationId={}]", divergenceType, correlationId);

        List<String> tags = new ArrayList<>();
        if (marketContext.getHistoricalCoverage() == HistoricalCoverage.INSUFFICIENT) {
            tags.add("LOW_HISTORICAL_CONFIDENCE");
        }

        return DivergenceReport.builder()
                .divergenceType(divergenceType)
                .primaryThesis(agent1.getThesis())
                .strongestCounterThesis(agent3.getThesis())
                .keyConflicts(buildKeyConflicts(contradictionMap))
                .historicalAnalogues(marketContext.getHistoricalSimilarity())
                .historicalCoverage(marketContext.getHistoricalCoverage())
                .tags(tags)
                .build();
    }

    private DivergenceType classifyDivergence(AgentOutput agent3, ContradictionMap contradictionMap) {
        boolean hasConflicts = !contradictionMap.getDirectionalConflicts().isEmpty()
                || !contradictionMap.getFlaggedTensions().isEmpty();

        if (agent3.getStatus() == AgentStatus.DEGRADED || agent3.getDivergenceAssessment() == DivergenceAssessment.DATA_GAP) {
            return DivergenceType.INSUFFICIENT_SIGNAL;
        }

        if (agent3.getDivergenceAssessment() == DivergenceAssessment.LOW_VARIANCE && !hasConflicts) {
            return DivergenceType.CONSENSUS_HIGH_CONVICTION;
        }

        if (agent3.getDivergenceAssessment() == DivergenceAssessment.SUBSTANTIVE && hasConflicts) {
            return DivergenceType.GENUINE_DIVERGENCE;
        }

        if (agent3.getDivergenceAssessment() == DivergenceAssessment.SUBSTANTIVE && !hasConflicts) {
            return DivergenceType.EMPHASIS_DISPUTE;
        }

        return DivergenceType.INSUFFICIENT_SIGNAL;
    }

    private List<Object> buildKeyConflicts(ContradictionMap contradictionMap) {
        List<Object> conflicts = new ArrayList<>();
        conflicts.addAll(contradictionMap.getDirectionalConflicts());
        conflicts.addAll(contradictionMap.getFlaggedTensions());
        return conflicts;
    }
}