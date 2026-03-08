package com.vetplatform.reviewextractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.InterpretationValidatorService;
import com.vetplatform.reviewextractor.application.service.ExtractionValidatorService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpretationValidatorServiceTest {

    private InterpretationValidatorService validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validator = new InterpretationValidatorService();
    }

    @Test
    void shouldPassWithValidInterpretation() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "symptoms": [
                    {"description": "Cojera en pata trasera", "suggested_code": "LAMENESS", "body_area": "LIMBS"}
                  ],
                  "urgency": "MODERATE",
                  "confidence": 0.82
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldFailWhenSymptomsNotArray() throws Exception {
        String json = """
                {
                  "symptoms": "dolor abdominal",
                  "urgency": "LOW",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("symptoms") && e.contains("array")));
    }

    @Test
    void shouldFailWhenSymptomMissingDescription() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"suggested_code": "VOMITING"}
                  ],
                  "urgency": "LOW",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("description")));
    }

    @Test
    void shouldFailWhenSymptomMissingSuggestedCode() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Vomito frecuente"}
                  ],
                  "urgency": "LOW",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("suggested_code")));
    }

    @Test
    void shouldFailWhenUrgencyMissing() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Tos", "suggested_code": "COUGH"}
                  ],
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("urgency")));
    }

    @Test
    void shouldFailWhenUrgencyInvalid() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Tos", "suggested_code": "COUGH"}
                  ],
                  "urgency": "CRITICAL",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Urgency invalido")));
    }

    @Test
    void shouldFailWhenConfidenceMissing() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Tos", "suggested_code": "COUGH"}
                  ],
                  "urgency": "LOW"
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("confidence")));
    }

    @Test
    void shouldFailWhenConfidenceOutOfRange() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Tos", "suggested_code": "COUGH"}
                  ],
                  "urgency": "LOW",
                  "confidence": 1.5
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Confidence fuera de rango")));
    }

    @Test
    void shouldWarnOnLowConfidence() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Tos leve", "suggested_code": "COUGH"}
                  ],
                  "urgency": "LOW",
                  "confidence": 0.3
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("LOW_CONFIDENCE")));
    }

    @Test
    void shouldWarnOnUnrecognizedSpecies() throws Exception {
        String json = """
                {
                  "species": "DRAGON",
                  "symptoms": [
                    {"description": "Fuego", "suggested_code": "FIRE"}
                  ],
                  "urgency": "HIGH",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Especie no reconocida")));
    }

    @Test
    void shouldWarnOnInvalidBodyArea() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Dolor", "suggested_code": "PAIN", "body_area": "FOOT"}
                  ],
                  "urgency": "LOW",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("body_area no reconocida")));
    }

    @Test
    void shouldWarnOnInvalidSeverity() throws Exception {
        String json = """
                {
                  "symptoms": [
                    {"description": "Dolor", "suggested_code": "PAIN", "severity": "CRITICAL"}
                  ],
                  "urgency": "LOW",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("severity no reconocida")));
    }

    @Test
    void shouldWarnWhenSymptomsEmpty() throws Exception {
        String json = """
                {
                  "symptoms": [],
                  "urgency": "LOW",
                  "confidence": 0.7
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Lista de sintomas vacia")));
    }
}
