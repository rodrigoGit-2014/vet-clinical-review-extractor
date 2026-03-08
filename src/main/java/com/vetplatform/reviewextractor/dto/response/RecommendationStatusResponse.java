package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Estado del procesamiento de una recomendacion")
public record RecommendationStatusResponse(
        @Schema(description = "UUID de la solicitud")
        UUID requestId,

        @Schema(description = "Estado actual")
        String status,

        @Schema(description = "Timestamp de creacion")
        Instant createdAt,

        @Schema(description = "Timestamp de ultima actualizacion")
        Instant updatedAt,

        @Schema(description = "Paso actual del procesamiento")
        String currentStep,

        @Schema(description = "Pasos completados")
        List<String> stepsCompleted,

        @Schema(description = "Cantidad de reintentos")
        Integer retryCount,

        @Schema(description = "Razon del fallo", nullable = true)
        String failureReason,

        @Schema(description = "Mensaje de error", nullable = true)
        String failureMessage,

        @Schema(description = "Si la solicitud puede reprocesarse")
        Boolean retriable
) {}
