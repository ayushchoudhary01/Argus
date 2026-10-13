package com.argus.backend.client;

import com.argus.backend.exceptions.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class OllamaClient {

    private final RestClient restClient;

    @Value("${argus.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${argus.ollama.model}")
    private String model;

    public OllamaClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String generate(String prompt) {
        OllamaRequest request = new OllamaRequest(model, prompt, false);

        log.info("Calling Ollama model={}", model);

        OllamaResponse response;
        try {
            response = restClient.post()
                .uri(ollamaBaseUrl + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new PipelineException("Ollama call failed with status: " + res.getStatusCode(), PipelineException.UNKNOWN);
                })
                .body(OllamaResponse.class);
        } catch (RestClientException e) {
            throw new PipelineException("Ollama unreachable: " + e.getMessage(),
                    PipelineException.UNKNOWN);
        }

        if (response == null || response.getResponse() == null) {
            throw new PipelineException("Ollama returned empty response", PipelineException.UNKNOWN);
        }

        String raw = response.getResponse().strip();
        if (raw.startsWith("```")) {
            raw = raw.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
        }
        return raw;
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {}

    private record OllamaResponse(String response) {
        public String getResponse() {
            return response;
        }
    }
}