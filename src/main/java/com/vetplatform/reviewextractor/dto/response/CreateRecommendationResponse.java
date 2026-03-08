package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Respuesta a la creacion de una solicitud de recomendacion")
public record CreateRecommendationResponse(
        @Schema(description = "UUID de la solicitud", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID requestId,

        @Schema(description = "Estado de la solicitud", example = "RECEIVED")
        String status,

        @Schema(description = "Timestamp de creacion")
        Instant createdAt,

        @Schema(description = "Links HATEOAS")
        Map<String, String> links
) {}
