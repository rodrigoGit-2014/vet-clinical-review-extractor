package com.vetplatform.reviewextractor.infrastructure.llm;

public interface LlmExtractionClient {

    LlmResponse extract(String systemPrompt, String userPrompt);

    String getProviderName();

    String getModelName();

    record LlmResponse(
            String content,
            int inputTokens,
            int outputTokens
    ) {}
}
