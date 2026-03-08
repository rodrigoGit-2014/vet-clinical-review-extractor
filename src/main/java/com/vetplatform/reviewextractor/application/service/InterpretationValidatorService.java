package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetplatform.reviewextractor.domain.enums.BodyArea;
import com.vetplatform.reviewextractor.domain.enums.Species;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class InterpretationValidatorService {

    private static final Set<String> VALID_URGENCIES = Set.of("LOW", "MODERATE", "HIGH", "EMERGENCY");
    private static final Set<String> VALID_SEVERITIES = Set.of("MILD", "MODERATE", "SEVERE");

    public ExtractionValidatorService.ValidationResult validate(JsonNode interpretedJson) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // symptoms must be an array
        if (!interpretedJson.has("symptoms")) {
            errors.add("Campo requerido ausente: symptoms");
        } else if (!interpretedJson.get("symptoms").isArray()) {
            errors.add("Campo symptoms debe ser un array");
        } else {
            JsonNode symptoms = interpretedJson.get("symptoms");
            if (symptoms.isEmpty()) {
                warnings.add("Lista de sintomas vacia");
            }
            for (int i = 0; i < symptoms.size(); i++) {
                JsonNode s = symptoms.get(i);
                if (!s.has("description") || s.get("description").isNull() || s.get("description").asText().isBlank()) {
                    errors.add("Sintoma " + i + ": description requerido");
                }
                if (!s.has("suggested_code") || s.get("suggested_code").isNull() || s.get("suggested_code").asText().isBlank()) {
                    errors.add("Sintoma " + i + ": suggested_code requerido");
                }
                if (s.has("body_area") && !s.get("body_area").isNull()) {
                    if (!BodyArea.isValid(s.get("body_area").asText())) {
                        warnings.add("Sintoma " + i + ": body_area no reconocida: " + s.get("body_area").asText());
                    }
                }
                if (s.has("severity") && !s.get("severity").isNull()) {
                    if (!VALID_SEVERITIES.contains(s.get("severity").asText())) {
                        warnings.add("Sintoma " + i + ": severity no reconocida: " + s.get("severity").asText());
                    }
                }
            }
        }

        // urgency required
        if (!interpretedJson.has("urgency") || interpretedJson.get("urgency").isNull()) {
            errors.add("Campo requerido ausente: urgency");
        } else {
            String urgency = interpretedJson.get("urgency").asText();
            if (!VALID_URGENCIES.contains(urgency)) {
                errors.add("Urgency invalido: " + urgency + ". Valores permitidos: " + VALID_URGENCIES);
            }
        }

        // confidence required, range [0.0, 1.0]
        if (!interpretedJson.has("confidence") || interpretedJson.get("confidence").isNull()) {
            errors.add("Campo requerido ausente: confidence");
        } else {
            double confidence = interpretedJson.get("confidence").asDouble(-1);
            if (confidence < 0.0 || confidence > 1.0) {
                errors.add("Confidence fuera de rango [0.0, 1.0]: " + confidence);
            } else if (confidence < 0.5) {
                warnings.add("LOW_CONFIDENCE: " + confidence);
            }
        }

        // species validation (warning only, not required)
        if (interpretedJson.has("species") && !interpretedJson.get("species").isNull()) {
            String species = interpretedJson.get("species").asText();
            if (!Species.isValid(species)) {
                warnings.add("Especie no reconocida: " + species);
            }
        }

        if (!errors.isEmpty()) {
            return ExtractionValidatorService.ValidationResult.failure(errors, warnings);
        }
        return new ExtractionValidatorService.ValidationResult(true, List.of(), warnings);
    }
}
