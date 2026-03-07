package com.vetplatform.reviewextractor.infrastructure.llm;

import com.vetplatform.reviewextractor.infrastructure.exception.LlmInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeLlmClient implements LlmExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeLlmClient.class);
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";

    private final WebClient webClient;
    private final String model;
    private final int timeoutSeconds;
    private final int maxRetries;

    public ClaudeLlmClient(
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model,
            @Value("${llm.timeout-seconds}") int timeoutSeconds,
            @Value("${llm.max-retries}") int maxRetries
    ) {
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.webClient = WebClient.builder()
                .baseUrl(CLAUDE_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public LlmResponse extract(String systemPrompt, String userPrompt) {
        int attempt = 0;
        LlmInvocationException lastException = null;

        while (attempt <= maxRetries) {
            attempt++;
            try {
                return doExtract(systemPrompt, userPrompt, attempt);
            } catch (LlmInvocationException e) {
                lastException = e;
                log.warn("Intento {}/{} fallido para LLM: {}", attempt, maxRetries + 1, e.getMessage());
                if (attempt <= maxRetries) {
                    sleepWithBackoff(attempt);
                }
            }
        }

        throw new LlmInvocationException(
                "Todos los intentos fallaron (" + (maxRetries + 1) + " intentos)",
                lastException
        );
    }

    @SuppressWarnings("unchecked")
    private LlmResponse doExtract(String systemPrompt, String userPrompt, int attempt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 2048,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            Map<String, Object> response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null) {
                throw new LlmInvocationException("Respuesta nula del LLM (intento " + attempt + ")");
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                throw new LlmInvocationException("Respuesta sin contenido del LLM (intento " + attempt + ")");
            }

            String text = (String) content.get(0).get("text");
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            int inputTokens = usage != null ? ((Number) usage.getOrDefault("input_tokens", 0)).intValue() : 0;
            int outputTokens = usage != null ? ((Number) usage.getOrDefault("output_tokens", 0)).intValue() : 0;

            return new LlmResponse(text, inputTokens, outputTokens);

        } catch (WebClientResponseException e) {
            throw new LlmInvocationException(
                    "Error HTTP " + e.getStatusCode() + " del LLM (intento " + attempt + "): " + e.getResponseBodyAsString(),
                    e
            );
        } catch (LlmInvocationException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmInvocationException(
                    "Error inesperado en LLM (intento " + attempt + "): " + e.getMessage(),
                    e
            );
        }
    }

    private void sleepWithBackoff(int attempt) {
        try {
            long sleepMs = (long) Math.pow(2, attempt) * 1000;
            Thread.sleep(Math.min(sleepMs, 10000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getProviderName() {
        return "CLAUDE";
    }

    @Override
    public String getModelName() {
        return model;
    }
}
