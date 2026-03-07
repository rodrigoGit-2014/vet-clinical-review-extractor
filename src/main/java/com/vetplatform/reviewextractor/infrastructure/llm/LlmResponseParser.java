package com.vetplatform.reviewextractor.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LlmResponseParser {

    private static final Logger log = LoggerFactory.getLogger(LlmResponseParser.class);
    private final ObjectMapper objectMapper;

    public LlmResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parse(String rawResponse) throws JsonProcessingException {
        String cleaned = cleanResponse(rawResponse);
        return objectMapper.readTree(cleaned);
    }

    public boolean isValidJson(String rawResponse) {
        try {
            String cleaned = cleanResponse(rawResponse);
            objectMapper.readTree(cleaned);
            return true;
        } catch (JsonProcessingException e) {
            log.warn("JSON invalido recibido del LLM: {}", e.getMessage());
            return false;
        }
    }

    private String cleanResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();

        // Remove markdown code block wrappers if present
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }
}
