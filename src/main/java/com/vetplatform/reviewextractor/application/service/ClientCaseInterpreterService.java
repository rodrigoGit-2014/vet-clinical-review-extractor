package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmExtractionClient;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmExtractionClient.LlmResponse;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmResponseParser;
import com.vetplatform.reviewextractor.infrastructure.llm.PromptBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClientCaseInterpreterService {

    private static final Logger log = LoggerFactory.getLogger(ClientCaseInterpreterService.class);

    private final PromptBuilderService promptBuilder;
    private final LlmExtractionClient llmClient;
    private final LlmResponseParser responseParser;

    public ClientCaseInterpreterService(
            PromptBuilderService promptBuilder,
            LlmExtractionClient llmClient,
            LlmResponseParser responseParser
    ) {
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.responseParser = responseParser;
    }

    public record InterpretationResult(
            JsonNode interpretedJson,
            int inputTokens,
            int outputTokens,
            String systemPrompt,
            String userPrompt
    ) {}

    public InterpretationResult interpret(String clientText, String locale, String locationHint, String promptVersionOverride) throws JsonProcessingException {
        String effectiveVersion = promptBuilder.getEffectiveVersion(promptVersionOverride);
        String systemPrompt = promptBuilder.buildMatchingSystemPrompt(effectiveVersion);
        String userPrompt = promptBuilder.buildMatchingUserPrompt(clientText, locale, locationHint, effectiveVersion);

        LlmResponse llmResponse = llmClient.extract(systemPrompt, userPrompt);

        JsonNode parsed = parseWithRetry(llmResponse.content(), systemPrompt);

        return new InterpretationResult(
                parsed,
                llmResponse.inputTokens(),
                llmResponse.outputTokens(),
                systemPrompt,
                userPrompt
        );
    }

    private JsonNode parseWithRetry(String content, String systemPrompt) throws JsonProcessingException {
        if (responseParser.isValidJson(content)) {
            return responseParser.parse(content);
        }

        log.warn("Primera respuesta de interpretacion con JSON invalido, reintentando");

        String correctionPrompt = "Tu respuesta anterior no fue un JSON valido. " +
                "Por favor responde SOLO con el JSON estructurado, sin texto adicional.";
        LlmResponse retryResponse = llmClient.extract(
                systemPrompt,
                correctionPrompt + "\n\nTexto original de la respuesta:\n" + content
        );

        if (!responseParser.isValidJson(retryResponse.content())) {
            throw new JsonProcessingException("JSON invalido tras reintento de interpretacion") {};
        }

        return responseParser.parse(retryResponse.content());
    }

    public String getProviderName() {
        return llmClient.getProviderName();
    }

    public String getModelName() {
        return llmClient.getModelName();
    }
}
