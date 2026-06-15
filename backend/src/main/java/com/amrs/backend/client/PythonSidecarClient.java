package com.amrs.backend.client;

import com.amrs.backend.dto.Assumption;
import com.amrs.backend.dto.CausalChain;
import com.amrs.backend.dto.NLIRequest;
import com.amrs.backend.exceptions.PipelineException;
import com.amrs.backend.model.AgentOutput;
import com.amrs.backend.model.ContradictionMap;
import com.amrs.backend.model.EventRequest;
import com.amrs.backend.model.MarketContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PythonSidecarClient {

    private final RestClient restClient;

    @Value("${amrs.sidecar.base-url}")
    private String sidecarBaseUrl;

    public MarketContext fetchMarketContext(EventRequest request) {
        log.info("Calling sidecar /analytics event='{}'", request.getEvent());
        return restClient.post()
                .uri(sidecarBaseUrl + "/analytics")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new PipelineException("Sidecar /analytics failed with status: " + res.getStatusCode(), "unknown");
                })
                .body(MarketContext.class);
    }

    public ContradictionMap fetchContradictionMap(AgentOutput agent1, AgentOutput agent2, AgentOutput agent3) {
        NLIRequest nliRequest = NLIRequest.builder()
                .assumptions(mergeAssumptions(agent1, agent2, agent3))
                .causalChains(mergeCausalChains(agent1, agent2, agent3))
                .build();

        log.info("Calling sidecar /nli with {} assumptions and {} causal chains",
                nliRequest.getAssumptions().size(), nliRequest.getCausalChains().size());

        return restClient.post()
                .uri(sidecarBaseUrl + "/nli")
                .contentType(MediaType.APPLICATION_JSON)
                .body(nliRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new PipelineException("Sidecar /nli failed with status: " + res.getStatusCode(), "unknown");
                })
                .body(ContradictionMap.class);
    }

    private List<Assumption> mergeAssumptions(AgentOutput... agents) {
        return Arrays.stream(agents)
                .filter(a -> a.getExplicitAssumptions() != null)
                .flatMap(a -> a.getExplicitAssumptions().stream())
                .toList();
    }

    private List<CausalChain> mergeCausalChains(AgentOutput... agents) {
        return Arrays.stream(agents)
                .filter(a -> a.getCausalChain() != null)
                .map(AgentOutput::getCausalChain)
                .toList();
    }
}