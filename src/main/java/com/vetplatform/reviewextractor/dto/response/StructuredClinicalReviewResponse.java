package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Resultado completo de la extraccion y estructuracion de una resena clinica")
public record StructuredClinicalReviewResponse(
        @Schema(description = "UUID de la resena", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
        UUID reviewId,

        @Schema(description = "Estado de la resena", example = "COMPLETED")
        String status,

        @Schema(description = "Datos clinicos extraidos y normalizados")
        ExtractedData extractedData,

        @Schema(description = "Metadata del procesamiento LLM")
        ProcessingMetadata processingMetadata
) {
    @Schema(description = "Datos clinicos estructurados extraidos de la resena")
    public record ExtractedData(
            @Schema(description = "Especie del animal", example = "DOG",
                    allowableValues = {"DOG", "CAT", "BIRD", "RABBIT", "HAMSTER", "FISH", "REPTILE", "HORSE", "TURTLE", "GUINEA_PIG", "FERRET", "OTHER"})
            String species,

            @Schema(description = "Raza del animal (si se menciona)", example = "Labrador", nullable = true)
            String breed,

            @Schema(description = "Nombre de la mascota (si se menciona)", example = "Max", nullable = true)
            String petName,

            @Schema(description = "Lista de sintomas extraidos y normalizados")
            List<Symptom> symptoms,

            @Schema(description = "Lista de procedimientos veterinarios")
            List<Procedure> procedures,

            @Schema(description = "Lista de medicamentos mencionados")
            List<Medication> medications,

            @Schema(description = "Datos del veterinario mencionado")
            Veterinarian veterinarian,

            @Schema(description = "Ubicacion geografica mencionada")
            Location location,

            @Schema(description = "Resultado del tratamiento")
            Outcome outcome,

            @Schema(description = "Confianza global de la extraccion (0.0 a 1.0)", example = "0.92")
            Double overallConfidence,

            @Schema(description = "Timestamp de cuando se completo la extraccion")
            Instant extractionTimestamp
    ) {}

    @Schema(description = "Sintoma clinico extraido")
    public record Symptom(
            @Schema(description = "Descripcion del sintoma tal como lo escribio el dueno", example = "Bulto en zona anal")
            String description,

            @Schema(description = "Codigo normalizado del sintoma", example = "MASS")
            String normalizedCode,

            @Schema(description = "Area corporal afectada", example = "ANAL",
                    allowableValues = {"HEAD", "NECK", "THORAX", "ABDOMEN", "LIMBS", "ANAL", "SKIN", "ORAL", "OCULAR", "AURICULAR", "GENERAL"},
                    nullable = true)
            String bodyArea
    ) {}

    @Schema(description = "Procedimiento veterinario extraido")
    public record Procedure(
            @Schema(description = "Descripcion del procedimiento", example = "Cirugia")
            String description,

            @Schema(description = "Codigo normalizado del procedimiento", example = "SURGICAL_PROCEDURE")
            String normalizedCode,

            @Schema(description = "Tipo de procedimiento", example = "SURGICAL",
                    allowableValues = {"SURGICAL", "DIAGNOSTIC", "THERAPEUTIC", "PREVENTIVE"}, nullable = true)
            String type
    ) {}

    @Schema(description = "Medicamento mencionado en la resena")
    public record Medication(
            @Schema(description = "Nombre del medicamento", example = "Amoxicilina")
            String name,

            @Schema(description = "Dosis", example = "500mg", nullable = true)
            String dosage,

            @Schema(description = "Frecuencia de administracion", example = "cada 12 horas", nullable = true)
            String frequency
    ) {}

    @Schema(description = "Datos del veterinario mencionado")
    public record Veterinarian(
            @Schema(description = "Nombre del veterinario", example = "Dr. Perez", nullable = true)
            String name,

            @Schema(description = "Nombre de la clinica", example = "Clinica Veterinaria Talca", nullable = true)
            String clinic
    ) {}

    @Schema(description = "Ubicacion geografica extraida")
    public record Location(
            @Schema(description = "Texto original de ubicacion", example = "Talca", nullable = true)
            String raw,

            @Schema(description = "Ciudad normalizada", example = "Talca", nullable = true)
            String normalized,

            @Schema(description = "Region", example = "Maule", nullable = true)
            String region,

            @Schema(description = "Codigo pais ISO 2 letras", example = "CL", nullable = true)
            String country
    ) {}

    @Schema(description = "Resultado del tratamiento veterinario")
    public record Outcome(
            @Schema(description = "Estado del resultado", example = "FULLY_RECOVERED",
                    allowableValues = {"FULLY_RECOVERED", "IMPROVING", "STABLE", "WORSENING", "DECEASED", "UNKNOWN"})
            String status,

            @Schema(description = "Descripcion del resultado", example = "Completamente recuperado", nullable = true)
            String description
    ) {}

    @Schema(description = "Metadata del procesamiento LLM")
    public record ProcessingMetadata(
            @Schema(description = "Proveedor del LLM utilizado", example = "CLAUDE")
            String llmProvider,

            @Schema(description = "Modelo especifico del LLM", example = "claude-sonnet-4-20250514")
            String llmModel,

            @Schema(description = "Version del prompt utilizado", example = "v1.0")
            String promptVersion,

            @Schema(description = "Duracion total del procesamiento en milisegundos", example = "4823")
            Long processingDurationMs,

            @Schema(description = "Uso de tokens del LLM")
            TokenUsage tokenUsage
    ) {}

    @Schema(description = "Detalle de tokens consumidos por el LLM")
    public record TokenUsage(
            @Schema(description = "Tokens de entrada (prompt)", example = "387")
            Integer inputTokens,

            @Schema(description = "Tokens de salida (respuesta)", example = "256")
            Integer outputTokens
    ) {}
}
