package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vetplatform.reviewextractor.domain.entity.NormalizationSynonym;
import com.vetplatform.reviewextractor.domain.repository.NormalizationSynonymRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClinicalNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(ClinicalNormalizationService.class);

    private final NormalizationSynonymRepository synonymRepository;
    private final ObjectMapper objectMapper;

    // Cache: category -> (raw_term_lowercase -> normalized_code)
    private final Map<String, Map<String, String>> synonymCache = new ConcurrentHashMap<>();

    public ClinicalNormalizationService(NormalizationSynonymRepository synonymRepository, ObjectMapper objectMapper) {
        this.synonymRepository = synonymRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadSynonyms() {
        List<NormalizationSynonym> synonyms = synonymRepository.findByActiveTrue();
        synonymCache.clear();
        for (NormalizationSynonym s : synonyms) {
            synonymCache
                    .computeIfAbsent(s.getCategory(), k -> new HashMap<>())
                    .put(s.getRawTerm().toLowerCase().trim(), s.getNormalizedCode());
        }
        log.info("Cargados {} sinonimos de normalizacion en {} categorias",
                synonyms.size(), synonymCache.size());
    }

    public JsonNode normalize(JsonNode extractedJson) {
        ObjectNode normalized = extractedJson.deepCopy();

        normalizeSpecies(normalized);
        normalizeSymptoms(normalized);
        normalizeProcedures(normalized);
        normalizeOutcome(normalized);

        return normalized;
    }

    private void normalizeSpecies(ObjectNode json) {
        if (json.has("species") && !json.get("species").isNull()) {
            String species = json.get("species").asText();
            String normalizedSpecies = resolveCode("ANIMAL_TYPE", species);
            json.put("species", normalizedSpecies);
        }
    }

    private void normalizeSymptoms(ObjectNode json) {
        JsonNode symptoms = json.get("symptoms");
        if (symptoms != null && symptoms.isArray()) {
            ArrayNode normalizedSymptoms = objectMapper.createArrayNode();
            for (JsonNode symptom : symptoms) {
                ObjectNode s = symptom.deepCopy();
                if (s.has("suggested_code") && !s.get("suggested_code").isNull()) {
                    String code = s.get("suggested_code").asText();
                    s.put("normalized_code", resolveCode("SYMPTOM", code));
                }
                if (s.has("body_area") && !s.get("body_area").isNull()) {
                    String bodyArea = s.get("body_area").asText();
                    s.put("body_area", resolveCode("BODY_AREA", bodyArea));
                }
                normalizedSymptoms.add(s);
            }
            json.set("symptoms", normalizedSymptoms);
        }
    }

    private void normalizeProcedures(ObjectNode json) {
        JsonNode procedures = json.get("procedures");
        if (procedures != null && procedures.isArray()) {
            ArrayNode normalizedProcedures = objectMapper.createArrayNode();
            for (JsonNode procedure : procedures) {
                ObjectNode p = procedure.deepCopy();
                if (p.has("suggested_code") && !p.get("suggested_code").isNull()) {
                    String code = p.get("suggested_code").asText();
                    p.put("normalized_code", resolveCode("PROCEDURE", code));
                }
                normalizedProcedures.add(p);
            }
            json.set("procedures", normalizedProcedures);
        }
    }

    private void normalizeOutcome(ObjectNode json) {
        JsonNode outcome = json.get("outcome");
        if (outcome != null && outcome.isObject()) {
            ObjectNode o = outcome.deepCopy();
            if (o.has("status") && !o.get("status").isNull()) {
                String status = o.get("status").asText();
                o.put("status", resolveCode("OUTCOME", status));
            }
            json.set("outcome", o);
        }
    }

    public String resolveCode(String category, String rawValue) {
        if (rawValue == null) return null;

        String lowered = rawValue.toLowerCase().trim();

        // Try exact match in cache
        Map<String, String> categoryMap = synonymCache.getOrDefault(category, Map.of());
        String match = categoryMap.get(lowered);
        if (match != null) return match;

        // Try if the value itself is already a valid code (uppercase)
        String uppered = rawValue.toUpperCase().trim();
        if (categoryMap.containsValue(uppered)) return uppered;

        // Fuzzy match: check if any key contains the raw value or vice versa
        for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
            if (entry.getKey().contains(lowered) || lowered.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        log.warn("Termino no encontrado en categoria {}: '{}'. Se marca como UNMATCHED.", category, rawValue);
        return "UNMATCHED:" + rawValue;
    }
}
