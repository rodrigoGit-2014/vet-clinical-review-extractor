package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Respuesta a la creacion de una resena clinica")
public record CreateClinicalReviewResponse(
        @Schema(description = "UUID de la resena creada", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
        UUID reviewId,

        @Schema(description = "Estado inicial de la resena", example = "RECEIVED")
        String status,

        @Schema(description = "Timestamp de creacion")
        Instant createdAt,

        @Schema(description = "Links HATEOAS para consultar estado y resultado",
                example = "{\"self\": \"/api/v1/clinical-reviews/b7e2c4a1-...\", \"status\": \"/api/v1/clinical-reviews/b7e2c4a1-.../status\", \"result\": \"/api/v1/clinical-reviews/b7e2c4a1-.../result\"}")
        Map<String, String> links
) {}
