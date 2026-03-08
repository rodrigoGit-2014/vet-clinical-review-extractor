package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Respuesta al recalculo de una recomendacion")
public record RecalculateRecommendationResponse(
        @Schema(description = "UUID de la solicitud")
        UUID requestId,

        @Schema(description = "Estado anterior")
        String previousStatus,

        @Schema(description = "Nuevo estado")
        String newStatus,

        @Schema(description = "Razon del recalculo")
        String reason,

        @Schema(description = "Cantidad de recalculos realizados")
        Integer recalculateCount,

        @Schema(description = "Timestamp de actualizacion")
        Instant updatedAt
) {}
