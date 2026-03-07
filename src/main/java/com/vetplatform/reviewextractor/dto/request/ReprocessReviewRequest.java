package com.vetplatform.reviewextractor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para reprocesar una resena clinica")
public record ReprocessReviewRequest(
        @Schema(
                description = "Razon del reprocesamiento",
                example = "Prompt actualizado a v1.1 con mejor extraccion de medicamentos"
        )
        @Size(max = 500, message = "La razon no puede superar 500 caracteres")
        String reason,

        @Schema(
                description = "Si es true, fuerza el reproceso de una resena ya completada. Requerido si la resena esta en estado COMPLETED",
                example = "false",
                defaultValue = "false"
        )
        boolean forceReprocess,

        @Schema(
                description = "Version del prompt a utilizar. Si no se especifica, se usa la version activa en configuracion",
                example = "v1.1",
                pattern = "^v\\d+\\.\\d+$"
        )
        @Pattern(regexp = "^v\\d+\\.\\d+$", message = "Formato de version invalido. Ejemplo: v1.0")
        String promptVersionOverride
) {}
