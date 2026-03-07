package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Estado actual del procesamiento de una resena clinica")
public record ReviewProcessingStatusResponse(
        @Schema(description = "UUID de la resena", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
        UUID reviewId,

        @Schema(description = "Estado actual", example = "PROCESSING",
                allowableValues = {"RECEIVED", "PROCESSING", "EXTRACTION_COMPLETED", "VALIDATION_PASSED",
                        "VALIDATION_FAILED", "NORMALIZATION_FAILED", "COMPLETED", "FAILED", "REPROCESSING"})
        String status,

        @Schema(description = "Timestamp de creacion de la resena")
        Instant createdAt,

        @Schema(description = "Timestamp de la ultima actualizacion")
        Instant updatedAt,

        @Schema(description = "Paso actual del procesamiento", example = "LLM_INVOCATION",
                allowableValues = {"RECEIVED", "LLM_INVOCATION", "VALIDATION", "NORMALIZATION", "COMPLETED", "FAILED"})
        String currentStep,

        @Schema(description = "Lista de pasos ya completados", example = "[\"RECEIVED\", \"PROCESSING\"]")
        List<String> stepsCompleted,

        @Schema(description = "Cantidad de reintentos realizados", example = "0")
        Integer retryCount,

        @Schema(description = "Codigo de error si fallo", example = "LLM_INVOCATION_ERROR", nullable = true)
        String failureReason,

        @Schema(description = "Mensaje descriptivo del error", nullable = true)
        String failureMessage,

        @Schema(description = "Indica si la resena puede ser reprocesada", example = "true", nullable = true)
        Boolean retriable
) {}
