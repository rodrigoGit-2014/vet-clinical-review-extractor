package com.vetplatform.reviewextractor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.ClientCaseInterpreterService;
import com.vetplatform.reviewextractor.application.service.ClientCaseInterpreterService.InterpretationResult;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmExtractionClient;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmExtractionClient.LlmResponse;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmResponseParser;
import com.vetplatform.reviewextractor.infrastructure.llm.PromptBuilderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientCaseInterpreterServiceTest {

    @Mock
    private PromptBuilderService promptBuilder;

    @Mock
    private LlmExtractionClient llmClient;

    @Mock
    private LlmResponseParser responseParser;

    @InjectMocks
    private ClientCaseInterpreterService interpreterService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CLIENT_TEXT = "Mi perro tiene un bulto en la pata";
    private static final String LOCALE = "es-CL";
    private static final String LOCATION_HINT = "Santiago, Chile";
    private static final String VERSION_OVERRIDE = "v2.0";
    private static final String EFFECTIVE_VERSION = "v2.0";
    private static final String SYSTEM_PROMPT = "You are a veterinary assistant...";
    private static final String USER_PROMPT = "Analyze the following text...";
    private static final String VALID_JSON = """
            {"species": "DOG", "symptoms": [{"description": "bulto", "body_area": "LIMB"}]}""";
    private static final String INVALID_JSON = "This is not valid JSON at all";

    @Test
    void shouldInterpretSuccessfully() throws Exception {
        JsonNode expectedNode = objectMapper.readTree(VALID_JSON);

        when(promptBuilder.getEffectiveVersion(VERSION_OVERRIDE)).thenReturn(EFFECTIVE_VERSION);
        when(promptBuilder.buildMatchingSystemPrompt(EFFECTIVE_VERSION)).thenReturn(SYSTEM_PROMPT);
        when(promptBuilder.buildMatchingUserPrompt(CLIENT_TEXT, LOCALE, LOCATION_HINT, EFFECTIVE_VERSION))
                .thenReturn(USER_PROMPT);
        when(llmClient.extract(SYSTEM_PROMPT, USER_PROMPT))
                .thenReturn(new LlmResponse(VALID_JSON, 100, 200));
        when(responseParser.isValidJson(VALID_JSON)).thenReturn(true);
        when(responseParser.parse(VALID_JSON)).thenReturn(expectedNode);

        InterpretationResult result = interpreterService.interpret(CLIENT_TEXT, LOCALE, LOCATION_HINT, VERSION_OVERRIDE);

        assertEquals(expectedNode, result.interpretedJson());
        assertEquals(100, result.inputTokens());
        assertEquals(200, result.outputTokens());
        assertEquals(SYSTEM_PROMPT, result.systemPrompt());
        assertEquals(USER_PROMPT, result.userPrompt());

        verify(llmClient, times(1)).extract(anyString(), anyString());
    }

    @Test
    void shouldRetryWhenFirstResponseInvalidJson() throws Exception {
        String retryJson = """
                {"species": "DOG", "symptoms": []}""";
        JsonNode retryNode = objectMapper.readTree(retryJson);

        when(promptBuilder.getEffectiveVersion(VERSION_OVERRIDE)).thenReturn(EFFECTIVE_VERSION);
        when(promptBuilder.buildMatchingSystemPrompt(EFFECTIVE_VERSION)).thenReturn(SYSTEM_PROMPT);
        when(promptBuilder.buildMatchingUserPrompt(CLIENT_TEXT, LOCALE, LOCATION_HINT, EFFECTIVE_VERSION))
                .thenReturn(USER_PROMPT);
        when(llmClient.extract(eq(SYSTEM_PROMPT), eq(USER_PROMPT)))
                .thenReturn(new LlmResponse(INVALID_JSON, 100, 200));
        when(llmClient.extract(eq(SYSTEM_PROMPT), contains("no fue un JSON valido")))
                .thenReturn(new LlmResponse(retryJson, 110, 210));
        when(responseParser.isValidJson(INVALID_JSON)).thenReturn(false);
        when(responseParser.isValidJson(retryJson)).thenReturn(true);
        when(responseParser.parse(retryJson)).thenReturn(retryNode);

        InterpretationResult result = interpreterService.interpret(CLIENT_TEXT, LOCALE, LOCATION_HINT, VERSION_OVERRIDE);

        assertEquals(retryNode, result.interpretedJson());
        assertEquals(100, result.inputTokens());
        assertEquals(200, result.outputTokens());

        verify(llmClient, times(2)).extract(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenBothAttemptsInvalidJson() {
        when(promptBuilder.getEffectiveVersion(VERSION_OVERRIDE)).thenReturn(EFFECTIVE_VERSION);
        when(promptBuilder.buildMatchingSystemPrompt(EFFECTIVE_VERSION)).thenReturn(SYSTEM_PROMPT);
        when(promptBuilder.buildMatchingUserPrompt(CLIENT_TEXT, LOCALE, LOCATION_HINT, EFFECTIVE_VERSION))
                .thenReturn(USER_PROMPT);
        when(llmClient.extract(eq(SYSTEM_PROMPT), eq(USER_PROMPT)))
                .thenReturn(new LlmResponse(INVALID_JSON, 100, 200));
        when(llmClient.extract(eq(SYSTEM_PROMPT), contains("no fue un JSON valido")))
                .thenReturn(new LlmResponse(INVALID_JSON, 110, 210));
        when(responseParser.isValidJson(INVALID_JSON)).thenReturn(false);

        assertThrows(JsonProcessingException.class,
                () -> interpreterService.interpret(CLIENT_TEXT, LOCALE, LOCATION_HINT, VERSION_OVERRIDE));

        verify(llmClient, times(2)).extract(anyString(), anyString());
    }

    @Test
    void shouldDelegateProviderName() {
        when(llmClient.getProviderName()).thenReturn("claude");

        String providerName = interpreterService.getProviderName();

        assertEquals("claude", providerName);
        verify(llmClient).getProviderName();
    }

    @Test
    void shouldDelegateModelName() {
        when(llmClient.getModelName()).thenReturn("claude-sonnet-4-20250514");

        String modelName = interpreterService.getModelName();

        assertEquals("claude-sonnet-4-20250514", modelName);
        verify(llmClient).getModelName();
    }
}
