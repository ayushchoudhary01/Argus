package com.amrs.backend.service;

import com.amrs.backend.client.OllamaClient;
import com.amrs.backend.dto.AgentContext;
import com.amrs.backend.dto.HistoricalAnalogue;
import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.model.AgentOutput;
import com.amrs.backend.model.EventRequest;
import com.amrs.backend.model.MarketContext;
import com.amrs.backend.validation.AgentOutputValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final OllamaClient ollamaClient;
    private final AgentOutputValidator validator;
    private final ObjectMapper objectMapper;

    public AgentOutput runStructuralAgent(MarketContext marketContext, EventRequest request) {
        String prompt = buildStructuralPrompt(marketContext, request);
        return executeWithRetry("structural", prompt, buildStricterStructuralPrompt(marketContext, request));
    }

    public AgentOutput runRiskAgent(MarketContext marketContext, EventRequest request) {
        String prompt = buildRiskPrompt(marketContext, request);
        return executeWithRetry("risk", prompt, buildStricterRiskPrompt(marketContext, request));
    }

    public AgentOutput runContrarianAgent(AgentContext context) {
        String prompt = buildContrarianPrompt(context);
        return executeWithRetry("contrarian", prompt, buildStricterContrarianPrompt(context));
    }

    private AgentOutput executeWithRetry(String agent, String prompt, String stricterPrompt) {
        AgentOutput output = callOllama(agent, prompt);
        if (validator.isValid(output)) {
            return output;
        }
        log.warn("Agent {} failed validation — retrying with stricter prompt", agent);
        AgentOutput retryOutput = callOllama(agent, stricterPrompt);
        if (validator.isValid(retryOutput)) {
            return retryOutput;
        }
        log.error("Agent {} failed validation after retry — marking DEGRADED", agent);
        return degraded(agent, retryOutput);
    }

    private AgentOutput callOllama(String agent, String prompt) {
        try {
            String response = ollamaClient.generate(prompt);
            AgentOutput output = objectMapper.readValue(response, AgentOutput.class);
            return output;
        } catch (Exception e) {
            log.error("Ollama call failed for agent={}", agent, e);
            return degraded(agent, null);
        }
    }

    private AgentOutput degraded(String agent, AgentOutput partial) {
        if (partial == null) {
            return AgentOutput.builder()
                    .agent(agent)
                    .status(AgentStatus.DEGRADED)
                    .build();
        }
        return AgentOutput.builder()
                .agent(agent)
                .thesis(partial.getThesis())
                .reasoningChain(partial.getReasoningChain())
                .causalChain(partial.getCausalChain())
                .explicitAssumptions(partial.getExplicitAssumptions())
                .status(AgentStatus.DEGRADED)
                .build();
    }

    private String buildStructuralPrompt(MarketContext ctx, EventRequest request) {
        return """
                You are the Structural Agent in a multi-agent financial reasoning system.
                Your mandate: Identify what structural macro regime this event confirms or disrupts.
                You have access to: macro indicators and historical analogues.
                
                Event: %s
                
                Market Context:
                Yield Curve Slope: %s
                DXY Momentum: %s
                Historical Analogues: %s
                
                Respond ONLY with valid JSON matching this exact schema:
                {
                  "agent": "structural",
                  "thesis": "<your main conclusion>",
                  "reasoning_chain": ["<step1>", "<step2>", "<step3>"],
                  "causal_chain": {
                    "driver": "<primary driver>",
                    "direction": "UPWARD_PRESSURE or DOWNWARD_PRESSURE",
                    "target_variable": "<what is being pressured>",
                    "agent": "structural"
                  },
                  "explicit_assumptions": [
                    {"id": "S1", "text": "<assumption>", "agent": "structural"}
                  ],
                  "historical_refs": ["<ref1>"],
                  "key_uncertainties": ["<uncertainty1>"],
                  "status": "OK"
                }
                
                CRITICAL: reasoning_chain length must be >= explicit_assumptions length.
                Do not include any text outside the JSON object.
                """.formatted(
                request.getEvent(),
                ctx.getMacro().getYieldCurveSlope(),
                ctx.getMacro().getDxyMomentum(),
                formatHistoricalAnalogues(ctx.getHistoricalSimilarity())
        );
    }

    private String buildStricterStructuralPrompt(MarketContext ctx, EventRequest request) {
        return buildStructuralPrompt(ctx, request) + """
                
                STRICT MODE: Your previous response failed validation.
                You MUST include at least 3 items in reasoning_chain.
                You MUST include at least 1 item in explicit_assumptions.
                reasoning_chain length MUST be >= explicit_assumptions length.
                Return ONLY the JSON object. No markdown. No explanation.
                """;
    }

    private String buildRiskPrompt(MarketContext ctx, EventRequest request) {
        return """
                You are the Risk Agent in a multi-agent financial reasoning system.
                Your mandate: Identify tail risks and hidden dangers that consensus is not pricing.
                You have access to: volatility data and anomaly flags.
                
                Event: %s
                
                Market Context:
                VIX Level: %s (30d percentile: %s)
                Anomaly Flags: %s
                
                Respond ONLY with valid JSON matching this exact schema:
                {
                  "agent": "risk",
                  "thesis": "<your main conclusion>",
                  "reasoning_chain": ["<step1>", "<step2>", "<step3>"],
                  "causal_chain": {
                    "driver": "<primary driver>",
                    "direction": "UPWARD_PRESSURE or DOWNWARD_PRESSURE",
                    "target_variable": "<what is being pressured>",
                    "agent": "risk"
                  },
                  "explicit_assumptions": [
                    {"id": "R1", "text": "<assumption>", "agent": "risk"}
                  ],
                  "historical_refs": [],
                  "key_uncertainties": ["<uncertainty1>"],
                  "status": "OK"
                }
                
                CRITICAL: reasoning_chain length must be >= explicit_assumptions length.
                Do not include any text outside the JSON object.
                """.formatted(
                request.getEvent(),
                ctx.getVolatility().getVixLevel(),
                ctx.getVolatility().getPercentile30d(),
                ctx.getAnomalyFlags()
        );
    }

    private String buildStricterRiskPrompt(MarketContext ctx, EventRequest request) {
        return buildRiskPrompt(ctx, request) + """
                
                STRICT MODE: Your previous response failed validation.
                You MUST include at least 3 items in reasoning_chain.
                You MUST include at least 1 item in explicit_assumptions.
                reasoning_chain length MUST be >= explicit_assumptions length.
                Return ONLY the JSON object. No markdown. No explanation.
                """;
    }

    private String buildContrarianPrompt(AgentContext ctx) {
        return """
                You are the Contrarian Agent in a multi-agent financial reasoning system.
                Your mandate: Find the strongest case that both Agent 1 and Agent 2 are wrong.
                
                IMPORTANT ESCAPE HATCH: If the combined reasoning of Agent 1 and Agent 2 is internally
                consistent and well-supported by the data, output LOW_VARIANCE. Inventing disagreement
                where none exists is a reasoning failure, not a success.
                
                Market Context:
                Yield Curve: %s | DXY: %s | VIX: %s
                
                Agent 1 (Structural):
                Thesis: %s
                Reasoning: %s
                Causal Chain: %s -> %s on %s
                Assumptions: %s
                
                Agent 2 (Risk):
                Thesis: %s
                Reasoning: %s
                Causal Chain: %s -> %s on %s
                Assumptions: %s
                
                Respond ONLY with valid JSON matching this exact schema:
                {
                  "agent": "contrarian",
                  "divergence_assessment": "SUBSTANTIVE or LOW_VARIANCE or DATA_GAP",
                  "thesis": "<your counter thesis>",
                  "reasoning_chain": ["<step1>", "<step2>"],
                  "causal_chain": {
                    "driver": "<primary driver>",
                    "direction": "UPWARD_PRESSURE or DOWNWARD_PRESSURE",
                    "target_variable": "<what is being pressured>",
                    "agent": "contrarian"
                  },
                  "explicit_assumptions": [
                    {"id": "C1", "text": "<assumption>", "agent": "contrarian"}
                  ],
                  "challenged_assumptions": ["S1", "R1"],
                  "validated_assumptions": ["S2"],
                  "status": "OK"
                }
                
                Do not include any text outside the JSON object.
                """.formatted(
                ctx.getMarketContext().getMacro().getYieldCurveSlope(),
                ctx.getMarketContext().getMacro().getDxyMomentum(),
                ctx.getMarketContext().getVolatility().getVixLevel(),
                ctx.getAgent1Thesis(),
                ctx.getAgent1ReasoningChain(),
                ctx.getAgent1CausalChain().getDriver(),
                ctx.getAgent1CausalChain().getDirection(),
                ctx.getAgent1CausalChain().getTargetVariable(),
                ctx.getAgent1Assumptions(),
                ctx.getAgent2Thesis(),
                ctx.getAgent2ReasoningChain(),
                ctx.getAgent2CausalChain().getDriver(),
                ctx.getAgent2CausalChain().getDirection(),
                ctx.getAgent2CausalChain().getTargetVariable(),
                ctx.getAgent2Assumptions()
        );
    }

    private String buildStricterContrarianPrompt(AgentContext ctx) {
        return buildContrarianPrompt(ctx) + """
                
                STRICT MODE: Your previous response failed validation.
                Return ONLY the JSON object. No markdown. No explanation.
                """;
    }

    private String formatHistoricalAnalogues(List<HistoricalAnalogue> analogues) {
        if (analogues == null || analogues.isEmpty()) return "None available";
        return analogues.stream()
                .map(a -> "%s (similarity: %s) — %s".formatted(a.getTitle(), a.getSimilarityScore(), a.getWhatHappened()))
                .collect(Collectors.joining("\n"));
    }
}