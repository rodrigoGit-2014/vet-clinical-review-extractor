package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Respuesta de error estandarizada")
public record ErrorResponse(
        @Schema(description = "Codigo de error", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Mensaje descriptivo del error", example = "Error de validacion en el request")
        String message,

        @Schema(description = "Timestamp del error")
        Instant timestamp,

        @Schema(description = "Detalle de errores por campo (solo para errores de validacion)", nullable = true)
        List<FieldError> fieldErrors
) {
    @Schema(description = "Detalle de un error de validacion en un campo especifico")
    public record FieldError(
            @Schema(description = "Nombre del campo con error", example = "reviewText")
            String field,

            @Schema(description = "Valor rechazado", example = "", nullable = true)
            String rejectedValue,

            @Schema(description = "Mensaje de error del campo", example = "El campo 'reviewText' no puede estar vacio")
            String message
    ) {}

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now(), null);
    }

    public static ErrorResponse withFieldErrors(String error, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(error, message, Instant.now(), fieldErrors);
    }
}
