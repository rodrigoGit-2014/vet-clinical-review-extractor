package com.vetplatform.reviewextractor.infrastructure.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private PromptBuilderService promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilderService("v1.0");
    }

    @Test
    void shouldBuildSystemPrompt() {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("extraccion de datos clinicos veterinarios"));
        assertTrue(systemPrompt.contains("JSON"));
        assertTrue(systemPrompt.contains("species"));
    }

    @Test
    void shouldBuildUserPromptWithReplacements() {
        String userPrompt = promptBuilder.buildUserPrompt("Mi perro esta enfermo", "es-CL");
        assertNotNull(userPrompt);
        assertTrue(userPrompt.contains("Mi perro esta enfermo"));
        assertTrue(userPrompt.contains("es-CL"));
    }

    @Test
    void shouldReturnCorrectVersion() {
        assertEquals("v1.0", promptBuilder.getPromptVersion());
    }

    @Test
    void shouldUseOverrideVersion() {
        assertEquals("v2.0", promptBuilder.getEffectiveVersion("v2.0"));
        assertEquals("v1.0", promptBuilder.getEffectiveVersion(null));
        assertEquals("v1.0", promptBuilder.getEffectiveVersion(""));
    }
}
