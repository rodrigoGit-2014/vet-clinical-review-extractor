package com.vetplatform.reviewextractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.ClinicalNormalizationService;
import com.vetplatform.reviewextractor.domain.entity.NormalizationSynonym;
import com.vetplatform.reviewextractor.domain.repository.NormalizationSynonymRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalNormalizationServiceTest {

    @Mock
    private NormalizationSynonymRepository synonymRepository;

    private ClinicalNormalizationService normalizationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        normalizationService = new ClinicalNormalizationService(synonymRepository, objectMapper);

        // Set up mock synonyms
        when(synonymRepository.findByActiveTrue()).thenReturn(List.of(
                createSynonym("ANIMAL_TYPE", "perro", "DOG"),
                createSynonym("ANIMAL_TYPE", "gato", "CAT"),
                createSynonym("SYMPTOM", "bulto", "MASS"),
                createSynonym("SYMPTOM", "sangrado", "HEMORRHAGE"),
                createSynonym("BODY_AREA", "zona anal", "ANAL"),
                createSynonym("PROCEDURE", "cirugia", "SURGICAL_PROCEDURE"),
                createSynonym("OUTCOME", "recuperado", "FULLY_RECOVERED")
        ));

        normalizationService.loadSynonyms();
    }

    @Test
    void shouldNormalizeSpecies() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "symptoms": [],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 0.5
                }
                """;
        JsonNode result = normalizationService.normalize(objectMapper.readTree(json));
        assertEquals("DOG", result.get("species").asText());
    }

    @Test
    void shouldNormalizeSymptomsWithCodes() throws Exception {
        String json = """
                {
                  "species": "DOG",
                  "symptoms": [
                    {"description": "Bulto", "suggested_code": "MASS", "body_area": "ANAL"}
                  ],
                  "procedures": [],
                  "medications": [],
                  "veterinarian": {"name": null},
                  "location": {"raw": null},
                  "outcome": {"status": "UNKNOWN"},
                  "confidence": 0.8
                }
                """;
        JsonNode result = normalizationService.normalize(objectMapper.readTree(json));
        JsonNode firstSymptom = result.get("symptoms").get(0);
        assertNotNull(firstSymptom.get("normalized_code"));
    }

    @Test
    void shouldMarkUnmatchedTerms() {
        String result = normalizationService.resolveCode("SYMPTOM", "algo_desconocido");
        assertTrue(result.startsWith("UNMATCHED:"));
    }

    @Test
    void shouldResolveExactMatch() {
        assertEquals("DOG", normalizationService.resolveCode("ANIMAL_TYPE", "perro"));
        assertEquals("CAT", normalizationService.resolveCode("ANIMAL_TYPE", "gato"));
    }

    @Test
    void shouldResolveCaseInsensitive() {
        assertEquals("DOG", normalizationService.resolveCode("ANIMAL_TYPE", "PERRO"));
        assertEquals("MASS", normalizationService.resolveCode("SYMPTOM", "Bulto"));
    }

    @Test
    void shouldResolveAlreadyNormalizedCode() {
        assertEquals("DOG", normalizationService.resolveCode("ANIMAL_TYPE", "DOG"));
    }

    private NormalizationSynonym createSynonym(String category, String rawTerm, String normalizedCode) {
        NormalizationSynonym s = new NormalizationSynonym();
        s.setCategory(category);
        s.setRawTerm(rawTerm);
        s.setNormalizedCode(normalizedCode);
        s.setNormalizedLabel(normalizedCode);
        s.setActive(true);
        return s;
    }
}
