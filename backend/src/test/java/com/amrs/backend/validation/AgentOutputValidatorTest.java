package com.amrs.backend.validation;

import com.amrs.backend.dto.Assumption;
import com.amrs.backend.dto.CausalChain;
import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.enums.CausalDirection;
import com.amrs.backend.enums.DivergenceAssessment;
import com.amrs.backend.model.AgentOutput;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentOutputValidatorTest {

    private final AgentOutputValidator validator = new AgentOutputValidator();

    private CausalChain validCausalChain() {
        return CausalChain.builder()
                .driver("Persistent inflation")
                .direction(CausalDirection.UPWARD_PRESSURE)
                .targetVariable("Federal Reserve Terminal Rate")
                .agent("structural")
                .build();
    }

    private AgentOutput.AgentOutputBuilder validStructuralOutputBuilder() {
        return AgentOutput.builder()
                .agent("structural")
                .status(AgentStatus.OK)
                .thesis("Sticky inflation locks the Fed into a restrictive posture.")
                .reasoningChain(List.of("Step one", "Step two", "Step three"))
                .explicitAssumptions(List.of(
                        Assumption.builder().id("S1").text("Assumption one").agent("structural").build()
                ))
                .causalChain(validCausalChain());
    }

    @Test
    void nullOutput_isInvalid() {
        assertThat(validator.isValid(null)).isFalse();
    }

    @Test
    void alreadyDegradedOutput_isInvalid() {
        AgentOutput output = AgentOutput.builder()
                .agent("structural")
                .status(AgentStatus.DEGRADED)
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void missingThesis_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder().thesis(null).build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void blankThesis_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder().thesis("   ").build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void emptyReasoningChain_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder()
                .reasoningChain(Collections.emptyList())
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void nullReasoningChain_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder()
                .reasoningChain(null)
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void emptyAssumptions_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder()
                .explicitAssumptions(Collections.emptyList())
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void reasoningChainShorterThanAssumptions_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder()
                .reasoningChain(List.of("Only one step"))
                .explicitAssumptions(List.of(
                        Assumption.builder().id("S1").text("First").agent("structural").build(),
                        Assumption.builder().id("S2").text("Second").agent("structural").build()
                ))
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void reasoningChainEqualLengthToAssumptions_isValid() {
        AgentOutput output = validStructuralOutputBuilder()
                .reasoningChain(List.of("Step one"))
                .explicitAssumptions(List.of(
                        Assumption.builder().id("S1").text("First").agent("structural").build()
                ))
                .build();

        assertThat(validator.isValid(output)).isTrue();
    }

    @Test
    void missingCausalChain_isInvalid() {
        AgentOutput output = validStructuralOutputBuilder()
                .causalChain(null)
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void contrarianAgent_missingDivergenceAssessment_isInvalid() {
        AgentOutput output = AgentOutput.builder()
                .agent("contrarian")
                .status(AgentStatus.OK)
                .thesis("Counter thesis")
                .reasoningChain(List.of("Step one"))
                .explicitAssumptions(List.of(
                        Assumption.builder().id("C1").text("Assumption").agent("contrarian").build()
                ))
                .causalChain(validCausalChain())
                .divergenceAssessment(null)
                .build();

        assertThat(validator.isValid(output)).isFalse();
    }

    @Test
    void contrarianAgent_withDivergenceAssessment_isValid() {
        AgentOutput output = AgentOutput.builder()
                .agent("contrarian")
                .status(AgentStatus.OK)
                .thesis("Counter thesis")
                .reasoningChain(List.of("Step one"))
                .explicitAssumptions(List.of(
                        Assumption.builder().id("C1").text("Assumption").agent("contrarian").build()
                ))
                .causalChain(validCausalChain())
                .divergenceAssessment(DivergenceAssessment.LOW_VARIANCE)
                .build();

        assertThat(validator.isValid(output)).isTrue();
    }

    @Test
    void nonContrarianAgent_doesNotRequireDivergenceAssessment() {
        AgentOutput output = validStructuralOutputBuilder()
                .divergenceAssessment(null)
                .build();

        assertThat(validator.isValid(output)).isTrue();
    }

    @Test
    void fullyValidStructuralOutput_isValid() {
        AgentOutput output = validStructuralOutputBuilder().build();

        assertThat(validator.isValid(output)).isTrue();
    }
}
