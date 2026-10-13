package com.argus.backend.client;

import com.argus.backend.dto.Assumption;
import com.argus.backend.dto.CausalChain;
import com.argus.backend.dto.NLIRequest;
import com.argus.backend.exceptions.PipelineException;
import com.argus.backend.model.AgentOutput;
import com.argus.backend.model.ContradictionMap;
import com.argus.backend.model.EventRequest;
import com.argus.backend.model.MarketContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PythonSidecarClient {

    private final RestClient restClient;

    @Value("${argus.sidecar.base-url}")
    private String sidecarBaseUrl;

    public MarketContext fetchMarketContext(EventRequest request) {
        log.info("Calling sidecar /analytics event='{}'", request.getEvent());
        MarketContext context;
        try {
            context = restClient.post()
                .uri(sidecarBaseUrl + "/analytics")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new PipelineException("Sidecar /analytics failed with status: " + res.getStatusCode(), PipelineException.UNKNOWN);
                })
                .body(MarketContext.class);
        } catch (RestClientException e) {
            // onStatus only fires when a status exists; a refused connection never gets one.
            throw new PipelineException("Sidecar /analytics unreachable: " + e.getMessage(),
                    PipelineException.UNKNOWN);
        }

        // A 2xx with an empty body deserialises to null, which would NPE in the prompt
        // builders rather than here. Fail at the boundary instead.
        if (context == null) {
            throw new PipelineException("Sidecar /analytics returned an empty body", PipelineException.UNKNOWN);
        }
        return context;
    }

    public ContradictionMap fetchContradictionMap(AgentOutput agent1, AgentOutput agent2, AgentOutput agent3) {
        NLIRequest nliRequest = NLIRequest.builder()
                .assumptions(mergeAssumptions(agent1, agent2, agent3))
                .causalChains(mergeCausalChains(agent1, agent2, agent3))
                .build();

        log.info("Calling sidecar /nli with {} assumptions and {} causal chains",
                nliRequest.getAssumptions().size(), nliRequest.getCausalChains().size());

        ContradictionMap map;
        try {
            map = restClient.post()
                .uri(sidecarBaseUrl + "/nli")
                .contentType(MediaType.APPLICATION_JSON)
                .body(nliRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new PipelineException("Sidecar /nli failed with status: " + res.getStatusCode(), PipelineException.UNKNOWN);
                })
                .body(ContradictionMap.class);
        } catch (RestClientException e) {
            throw new PipelineException("Sidecar /nli unreachable: " + e.getMessage(),
                    PipelineException.UNKNOWN);
        }

        if (map == null) {
            throw new PipelineException("Sidecar /nli returned an empty body", PipelineException.UNKNOWN);
        }
        return map;
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