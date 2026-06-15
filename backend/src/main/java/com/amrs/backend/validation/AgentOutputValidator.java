package com.amrs.backend.validation;

import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.model.AgentOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AgentOutputValidator {

    public boolean isValid(AgentOutput output) {
        if (output == null) {
            log.warn("Agent output is null");
            return false;
        }

        if (output.getStatus() == AgentStatus.DEGRADED) {
            log.warn("Agent {} already marked DEGRADED", output.getAgent());
            return false;
        }

        if (isBlank(output.getThesis())) {
            log.warn("Agent {} missing thesis", output.getAgent());
            return false;
        }

        if (isEmpty(output.getReasoningChain())) {
            log.warn("Agent {} missing reasoning chain", output.getAgent());
            return false;
        }

        if (isEmpty(output.getExplicitAssumptions())) {
            log.warn("Agent {} missing explicit assumptions", output.getAgent());
            return false;
        }

        if (output.getReasoningChain().size() < output.getExplicitAssumptions().size()) {
            log.warn("Agent {} reasoning chain length {} < assumptions length {}",
                    output.getAgent(),
                    output.getReasoningChain().size(),
                    output.getExplicitAssumptions().size());
            return false;
        }

        if (output.getCausalChain() == null) {
            log.warn("Agent {} missing causal chain", output.getAgent());
            return false;
        }

        if ("contrarian".equals(output.getAgent()) && output.getDivergenceAssessment() == null) {
            log.warn("Contrarian agent missing divergence assessment");
            return false;
        }

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
