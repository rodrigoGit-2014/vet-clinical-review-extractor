package com.vetplatform.reviewextractor.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmResponseParserTest {

    private LlmResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new LlmResponseParser(new ObjectMapper());
    }

    @Test
    void shouldParseValidJson() throws JsonProcessingException {
        String json = "{\"species\": \"DOG\", \"confidence\": 0.9}";
        JsonNode result = parser.parse(json);
        assertEquals("DOG", result.get("species").asText());
        assertEquals(0.9, result.get("confidence").asDouble());
    }

    @Test
    void shouldHandleMarkdownWrappedJson() throws JsonProcessingException {
        String json = "```json\n{\"species\": \"CAT\"}\n```";
        JsonNode result = parser.parse(json);
        assertEquals("CAT", result.get("species").asText());
    }

    @Test
    void shouldHandleGenericMarkdownWrapper() throws JsonProcessingException {
        String json = "```\n{\"species\": \"BIRD\"}\n```";
        JsonNode result = parser.parse(json);
        assertEquals("BIRD", result.get("species").asText());
    }

    @Test
    void shouldDetectInvalidJson() {
        assertFalse(parser.isValidJson("this is not json"));
        assertFalse(parser.isValidJson("{invalid json}"));
    }

    @Test
    void shouldDetectValidJson() {
        assertTrue(parser.isValidJson("{\"key\": \"value\"}"));
        assertTrue(parser.isValidJson("```json\n{\"key\": \"value\"}\n```"));
    }

    @Test
    void shouldHandleNullInput() throws JsonProcessingException {
        JsonNode result = parser.parse(null);
        assertTrue(result.isObject());
    }
}
