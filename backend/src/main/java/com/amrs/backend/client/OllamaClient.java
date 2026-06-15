package com.amrs.backend.client;

import com.amrs.backend.exceptions.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class OllamaClient {

    private final RestClient restClient;

    @Value("${amrs.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${amrs.ollama.model}")
    private String model;

    public OllamaClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String generate(String prompt) {
        OllamaRequest request = new OllamaRequest(model, prompt, false);

        log.info("Calling Ollama model={}", model);

        OllamaResponse response = restClient.post()
                .uri(ollamaBaseUrl + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new PipelineException("Ollama call failed with status: " + res.getStatusCode(), "unknown");
                })
                .body(OllamaResponse.class);

        if (response == null || response.getResponse() == null) {
            throw new PipelineException("Ollama returned empty response", "unknown");
        }

        return response.getResponse();
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {}

    private record OllamaResponse(String response) {
        public String getResponse() {
            return response;
        }
    }
}