package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Respuesta al reprocesamiento de una resena")
public record ReprocessReviewResponse(
        @Schema(description = "UUID de la resena", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
        UUID reviewId,

        @Schema(description = "Estado anterior de la resena", example = "FAILED")
        String previousStatus,

        @Schema(description = "Nuevo estado de la resena", example = "REPROCESSING")
        String newStatus,

        @Schema(description = "Razon del reprocesamiento", example = "Prompt actualizado a v1.1")
        String reprocessReason,

        @Schema(description = "Cantidad de veces que se ha reprocesado", example = "1")
        Integer reprocessCount,

        @Schema(description = "Timestamp de la actualizacion")
        Instant updatedAt
) {}
