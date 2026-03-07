package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetplatform.reviewextractor.domain.enums.BodyArea;
import com.vetplatform.reviewextractor.domain.enums.OutcomeStatus;
import com.vetplatform.reviewextractor.domain.enums.ProcedureType;
import com.vetplatform.reviewextractor.domain.enums.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExtractionValidatorService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionValidatorService.class);

    public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of(), List.of());
        }

        public static ValidationResult failure(List<String> errors, List<String> warnings) {
            return new ValidationResult(false, errors, warnings);
        }
    }

    public ValidationResult validate(JsonNode extractedJson) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Required fields
        validateRequiredField(extractedJson, "species", errors);
        validateRequiredField(extractedJson, "confidence", errors);
        validateRequiredArray(extractedJson, "symptoms", errors);
        validateRequiredArray(extractedJson, "procedures", errors);
        validateRequiredArray(extractedJson, "medications", errors);
        validateRequiredObject(extractedJson, "veterinarian", errors);
        validateRequiredObject(extractedJson, "location", errors);
        validateRequiredObject(extractedJson, "outcome", errors);

        // Enum validations
        validateSpecies(extractedJson, warnings);
        validateConfidenceRange(extractedJson, errors, warnings);
        validateSymptoms(extractedJson, warnings);
        validateProcedures(extractedJson, warnings);
        validateOutcome(extractedJson, warnings);

        // Content validation
        validateMinimumContent(extractedJson, warnings);

        // Truncate extraction_notes if too long
        validateExtractionNotes(extractedJson, warnings);

        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors, warnings);
        }

        return new ValidationResult(true, List.of(), warnings);
    }

    private void validateRequiredField(JsonNode json, String field, List<String> errors) {
        if (!json.has(field) || json.get(field).isNull()) {
            errors.add("Campo requerido ausente: " + field);
        }
    }

    private void validateRequiredArray(JsonNode json, String field, List<String> errors) {
        if (!json.has(field)) {
            errors.add("Campo requerido ausente: " + field);
        } else if (!json.get(field).isArray()) {
            errors.add("Campo " + field + " debe ser un array");
        }
    }

    private void validateRequiredObject(JsonNode json, String field, List<String> errors) {
        if (!json.has(field)) {
            errors.add("Campo requerido ausente: " + field);
        } else if (!json.get(field).isObject()) {
            errors.add("Campo " + field + " debe ser un objeto");
        }
    }

    private void validateSpecies(JsonNode json, List<String> warnings) {
        if (json.has("species") && !json.get("species").isNull()) {
            String species = json.get("species").asText();
            if (!Species.isValid(species)) {
                warnings.add("Especie no reconocida: " + species + ". Se usara null.");
            }
        }
    }

    private void validateConfidenceRange(JsonNode json, List<String> errors, List<String> warnings) {
        if (json.has("confidence") && !json.get("confidence").isNull()) {
            double confidence = json.get("confidence").asDouble(-1);
            if (confidence < 0.0 || confidence > 1.0) {
                errors.add("Confidence fuera de rango [0.0, 1.0]: " + confidence);
            } else if (confidence < 0.5) {
                warnings.add("LOW_CONFIDENCE: " + confidence);
            }
        }
    }

    private void validateSymptoms(JsonNode json, List<String> warnings) {
        JsonNode symptoms = json.get("symptoms");
        if (symptoms != null && symptoms.isArray()) {
            for (JsonNode symptom : symptoms) {
                if (symptom.has("body_area") && !symptom.get("body_area").isNull()) {
                    String bodyArea = symptom.get("body_area").asText();
                    if (!BodyArea.isValid(bodyArea)) {
                        warnings.add("body_area no reconocida: " + bodyArea);
                    }
                }
            }
        }
    }

    private void validateProcedures(JsonNode json, List<String> warnings) {
        JsonNode procedures = json.get("procedures");
        if (procedures != null && procedures.isArray()) {
            for (JsonNode procedure : procedures) {
                if (procedure.has("type") && !procedure.get("type").isNull()) {
                    String type = procedure.get("type").asText();
                    if (!ProcedureType.isValid(type)) {
                        warnings.add("Tipo de procedimiento no reconocido: " + type);
                    }
                }
            }
        }
    }

    private void validateOutcome(JsonNode json, List<String> warnings) {
        JsonNode outcome = json.get("outcome");
        if (outcome != null && outcome.isObject() && outcome.has("status") && !outcome.get("status").isNull()) {
            String status = outcome.get("status").asText();
            if (!OutcomeStatus.isValid(status)) {
                warnings.add("Outcome status no reconocido: " + status);
            }
        }
    }

    private void validateMinimumContent(JsonNode json, List<String> warnings) {
        boolean hasSymptoms = json.has("symptoms") && json.get("symptoms").isArray() && json.get("symptoms").size() > 0;
        boolean hasProcedures = json.has("procedures") && json.get("procedures").isArray() && json.get("procedures").size() > 0;
        boolean hasVetName = json.has("veterinarian") && json.get("veterinarian").isObject()
                && json.get("veterinarian").has("name") && !json.get("veterinarian").get("name").isNull();

        if (!hasSymptoms && !hasProcedures && !hasVetName) {
            warnings.add("Contenido minimo no alcanzado: sin sintomas, procedimientos ni veterinario");
        }

        if (!hasVetName) {
            warnings.add("Nombre de veterinario no encontrado en la resena");
        }
    }

    private void validateExtractionNotes(JsonNode json, List<String> warnings) {
        if (json.has("extraction_notes") && !json.get("extraction_notes").isNull()) {
            String notes = json.get("extraction_notes").asText();
            if (notes.length() > 500) {
                warnings.add("extraction_notes truncado a 500 caracteres");
            }
        }
    }
}
