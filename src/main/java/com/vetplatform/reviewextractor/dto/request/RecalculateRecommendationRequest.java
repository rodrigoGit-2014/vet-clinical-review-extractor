package com.vetplatform.reviewextractor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para recalcular una recomendacion de veterinario")
public record RecalculateRecommendationRequest(
        @Schema(description = "Razon del recalculo", example = "Se actualizaron resenas historicas")
        @Size(max = 500)
        String reason,

        @Schema(description = "Forzar recalculo de recomendacion completada")
        boolean forceRecalculate,

        @Schema(description = "Override de version del prompt", nullable = true)
        @Pattern(regexp = "^v\\d+\\.\\d+$")
        String promptVersionOverride
) {}
