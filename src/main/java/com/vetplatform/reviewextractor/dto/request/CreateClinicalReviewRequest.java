package com.vetplatform.reviewextractor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para crear y procesar una resena clinica veterinaria")
public record CreateClinicalReviewRequest(
        @Schema(
                description = "Texto libre de la resena clinica escrita por el dueno de la mascota",
                example = "Mi perro tenia un bulto en la zona anal que empezo a sangrar. Lo lleve al Dr. Perez en Talca y le hizo una cirugia. Ahora esta completamente recuperado.",
                minLength = 10,
                maxLength = 5000,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El campo 'reviewText' no puede estar vacio")
        @Size(min = 10, max = 5000, message = "El texto debe tener entre 10 y 5000 caracteres")
        String reviewText,

        @Schema(
                description = "Identificador del dueno de la mascota",
                example = "owner-abc-123",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El campo 'petOwnerId' es requerido")
        String petOwnerId,

        @Schema(
                description = "Locale del texto. Determina el idioma y regionalismos a interpretar",
                example = "es-CL",
                defaultValue = "es-CL",
                allowableValues = {"es-CL", "es-MX", "es-CO", "es-AR", "es-PE", "pt-BR"}
        )
        @Pattern(regexp = "^(es-CL|es-MX|es-CO|es-AR|es-PE|pt-BR)$",
                message = "Locale no soportado. Valores validos: es-CL, es-MX, es-CO, es-AR, es-PE, pt-BR")
        String locale
) {
    public CreateClinicalReviewRequest {
        if (locale == null || locale.isBlank()) {
            locale = "es-CL";
        }
    }
}
