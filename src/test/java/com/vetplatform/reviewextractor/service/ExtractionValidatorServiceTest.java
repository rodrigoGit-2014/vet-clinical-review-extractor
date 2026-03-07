package com.vetplatform.reviewextractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.ExtractionValidatorService;
import com.vetplatform.reviewextractor.application.service.ExtractionValidatorService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionValidatorServiceTest {

    private ExtractionValidatorService validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validator = new ExtractionValidatorService();
    }

    @Test
    void shouldPassWithValidCompleteJson() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "breed": null,
                  "pet_name": null,
                  "symptoms": [
                    {"description": "Bulto", "suggested_code": "MASS", "body_area": "ANAL"}
                  ],
                  "procedures": [
                    {"description": "Cirugia", "suggested_code": "SURGICAL_PROCEDURE", "type": "SURGICAL"}
                  ],
                  "medications": [],
                  "veterinarian": {"name": "Dr. Perez", "clinic": null},
                  "location": {"raw": "Talca", "city": "Talca", "region": "Maule", "country": "CL"},
                  "outcome": {"status": "FULLY_RECOVERED", "description": "Recuperado"},
                  "confidence": 0.92,
                  "extraction_notes": null
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldFailWhenSpeciesMissing() throws Exception {
        String json = """
                {
                  "symptoms": [],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": null, "clinic": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 0.1
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("species")));
    }

    @Test
    void shouldFailWhenConfidenceOutOfRange() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "symptoms": [],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": null, "clinic": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 1.5
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Confidence")));
    }

    @Test
    void shouldWarnOnLowConfidence() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "symptoms": [{"description": "algo", "suggested_code": "X", "body_area": "GENERAL"}],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": "Dr. X", "clinic": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
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
                  "symptoms": [{"description": "fuego", "suggested_code": "FIRE", "body_area": "ORAL"}],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": "Dr. Fantasy", "clinic": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 0.5
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Especie no reconocida")));
    }

    @Test
    void shouldWarnWhenNoVetName() throws Exception {
        String json = """
                {
                  "species": "CAT",
                  "symptoms": [{"description": "vomito", "suggested_code": "VOMITING", "body_area": "GENERAL"}],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": null, "clinic": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 0.6
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("veterinario")));
    }

    @Test
    void shouldWarnOnMinimumContentNotMet() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "symptoms": [],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": null, "clinic": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 0.1
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        ValidationResult result = validator.validate(node);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Contenido minimo")));
    }
}
