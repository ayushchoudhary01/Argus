package com.amrs.backend.service;

import com.amrs.backend.client.PythonSidecarClient;
import com.amrs.backend.dto.AgentContext;
import com.amrs.backend.enums.AgentStatus;
import com.amrs.backend.exceptions.PipelineException;
import com.amrs.backend.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestrationService {

    private final PythonSidecarClient sidecarClient;
    private final AgentService agentService;
    private final DivergenceService divergenceService;
    private final CacheService cacheService;
    private final PersistenceService persistenceService;
    private final ExecutorService persistenceExecutor;

    public DivergenceReport analyse(EventRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Pipeline start [correlationId={}] event='{}'", correlationId, request.getEvent());

        String cacheKey = buildCacheKey(request);
        DivergenceReport cached = cacheService.get(cacheKey);
        if (cached != null) {
            log.info("Cache hit [correlationId={}]", correlationId);
            return cached;
        }

        MarketContext marketContext = sidecarClient.fetchMarketContext(request);
        log.info("MarketContext received [correlationId={}] coverage={}", correlationId,
                marketContext.getHistoricalCoverage());

        AgentOutput agent1Output;
        AgentOutput agent2Output;
        try (var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<AgentOutput> agent1Future = CompletableFuture.supplyAsync(
                    () -> agentService.runStructuralAgent(marketContext, request),
                    virtualThreadExecutor);
            CompletableFuture<AgentOutput> agent2Future = CompletableFuture.supplyAsync(
                    () -> agentService.runRiskAgent(marketContext, request),
                    virtualThreadExecutor);

            agent1Output = agent1Future.get();
            agent2Output = agent2Future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PipelineException("Agent fan-out interrupted", correlationId);
        } catch (ExecutionException e) {
            throw new PipelineException("Agent fan-out failed: " + e.getCause().getMessage(), correlationId);
        }

        log.info("Agent 1 status={} Agent 2 status={} [correlationId={}]",
                agent1Output.getStatus(), agent2Output.getStatus(), correlationId);

        if (agent1Output.getStatus() == AgentStatus.DEGRADED && agent2Output.getStatus() == AgentStatus.DEGRADED) {
            log.error("Both agents DEGRADED — aborting pipeline [correlationId={}]", correlationId);
            throw new PipelineException("Both Structural and Risk agents failed validation", correlationId);
        }

        AgentContext agent3Context = buildAgent3Context(agent1Output, agent2Output, marketContext);
        AgentOutput agent3Output = agentService.runContrarianAgent(agent3Context);

        log.info("Agent 3 status={} assessment={} [correlationId={}]",
                agent3Output.getStatus(), agent3Output.getDivergenceAssessment(), correlationId);

        ContradictionMap contradictionMap = sidecarClient.fetchContradictionMap(
                agent1Output, agent2Output, agent3Output);

        DivergenceReport report = divergenceService.buildReport(
                agent1Output, agent2Output, agent3Output,
                contradictionMap, marketContext, correlationId);

        cacheService.put(cacheKey, report);
        CompletableFuture.runAsync(() -> persistenceService.persist(report, correlationId), persistenceExecutor);

        log.info("Pipeline complete [correlationId={}] divergenceType={}", correlationId, report.getDivergenceType());
        return report;
    }

    String buildCacheKey(EventRequest request) {
        String normalizedEvent = request.getEvent().trim().toLowerCase().replaceAll("\\s+", " ");
        String normalizedAssets = request.getAssetContext().stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.joining(","));
        String raw = normalizedEvent + normalizedAssets;
        return "amrs:analysis:" + DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private AgentContext buildAgent3Context(AgentOutput agent1, AgentOutput agent2, MarketContext marketContext) {
        return AgentContext.builder()
                .marketContext(marketContext)
                .agent1Thesis(agent1.getThesis())
                .agent1ReasoningChain(agent1.getReasoningChain())
                .agent1CausalChain(agent1.getCausalChain())
                .agent1Assumptions(agent1.getExplicitAssumptions())
                .agent2Thesis(agent2.getThesis())
                .agent2ReasoningChain(agent2.getReasoningChain())
                .agent2CausalChain(agent2.getCausalChain())
                .agent2Assumptions(agent2.getExplicitAssumptions())
                .build();
    }
}