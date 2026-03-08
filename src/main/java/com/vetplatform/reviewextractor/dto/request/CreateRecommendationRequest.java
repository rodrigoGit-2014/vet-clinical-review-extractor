package com.vetplatform.reviewextractor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request para crear una solicitud de recomendacion de veterinario")
public record CreateRecommendationRequest(
        @Schema(description = "Texto libre del dueno describiendo el problema de su mascota",
                example = "tengo mi perrito que hace dias que no come, solamente toma agua")
        @NotBlank @Size(min = 10, max = 5000)
        String clientText,

        @Schema(description = "Identificador del dueno de mascota", example = "owner-abc-123")
        @NotBlank
        String petOwnerId,

        @Schema(description = "Locale del texto", example = "es-CL")
        @Pattern(regexp = "^(es-CL|es-MX|es-CO|es-AR|es-PE|pt-BR)$")
        String locale,

        @Schema(description = "Pista de ubicacion del cliente", example = "Santiago, Chile", nullable = true)
        String locationHint,

        @Schema(description = "Cantidad maxima de veterinarios a recomendar", example = "5")
        @Min(1) @Max(20)
        Integer maxResults
) {
    public CreateRecommendationRequest {
        if (locale == null || locale.isBlank()) locale = "es-CL";
        if (maxResults == null) maxResults = 5;
    }
}
