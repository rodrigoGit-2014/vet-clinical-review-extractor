package com.vetplatform.reviewextractor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Resultado completo de la recomendacion de veterinarios")
public record RecommendationResultResponse(
        @Schema(description = "UUID de la solicitud")
        UUID requestId,

        @Schema(description = "Estado de la solicitud", example = "COMPLETED")
        String status,

        @Schema(description = "Interpretacion estructurada de la solicitud del cliente")
        Interpretation interpretation,

        @Schema(description = "Lista de veterinarios recomendados, ordenados por score")
        List<VetRecommendationDto> recommendations,

        @Schema(description = "Metadata del procesamiento")
        RecommendationMetadata metadata
) {
    @Schema(description = "Interpretacion de la solicitud del cliente")
    public record Interpretation(
            @Schema(description = "Especie del animal", example = "DOG")
            String species,

            @Schema(description = "Lista de sintomas interpretados")
            List<InterpretedSymptom> symptoms,

            @Schema(description = "Nivel de urgencia", example = "MODERATE")
            String urgency,

            @Schema(description = "Pista de condicion (NO es un diagnostico)", nullable = true)
            String conditionHint,

            @Schema(description = "Ubicacion interpretada")
            InterpretedLocation location,

            @Schema(description = "Confianza de la interpretacion", example = "0.82")
            Double confidence
    ) {}

    @Schema(description = "Sintoma interpretado del texto del cliente")
    public record InterpretedSymptom(
            @Schema(description = "Codigo normalizado del sintoma", example = "APPETITE_LOSS")
            String code,

            @Schema(description = "Etiqueta legible", example = "Perdida de apetito")
            String label,

            @Schema(description = "Area corporal afectada", example = "GENERAL", nullable = true)
            String bodyArea,

            @Schema(description = "Duracion mencionada", example = "varios dias", nullable = true)
            String duration
    ) {}

    @Schema(description = "Ubicacion interpretada")
    public record InterpretedLocation(
            @Schema(description = "Pais ISO 2 letras", example = "CL", nullable = true)
            String country,

            @Schema(description = "Region", example = "Metropolitana", nullable = true)
            String region,

            @Schema(description = "Ciudad", example = "Santiago", nullable = true)
            String city
    ) {}

    @Schema(description = "Veterinario recomendado con score y explicacion")
    public record VetRecommendationDto(
            @Schema(description = "Posicion en el ranking", example = "1")
            Integer rank,

            @Schema(description = "Nombre del veterinario", example = "Dr. Martinez")
            String vetName,

            @Schema(description = "Nombre de la clinica", example = "Clinica Animal Feliz", nullable = true)
            String vetClinic,

            @Schema(description = "Ubicacion del veterinario")
            VetLocation location,

            @Schema(description = "Score compuesto (0.0 a 1.0)", example = "0.84")
            Double score,

            @Schema(description = "Cantidad de casos similares encontrados", example = "3")
            Integer similarCasesCount,

            @Schema(description = "Tasa de resultados positivos", example = "1.0")
            Double positiveOutcomeRate,

            @Schema(description = "Similaridad promedio de los casos", example = "0.807")
            Double avgSimilarity,

            @Schema(description = "Explicacion de la recomendacion")
            VetExplanation explanation
    ) {}

    @Schema(description = "Ubicacion del veterinario")
    public record VetLocation(
            @Schema(description = "Pais", example = "CL", nullable = true)
            String country,

            @Schema(description = "Region", example = "Metropolitana", nullable = true)
            String region
    ) {}

    @Schema(description = "Explicacion de por que se recomienda este veterinario")
    public record VetExplanation(
            @Schema(description = "Resumen en texto natural", example = "Dr. Martinez ha tratado 3 casos similares de perdida de apetito en perros, con un 100% de resultados positivos en tu misma zona.")
            String summary,

            @Schema(description = "Sintomas que coincidieron")
            List<String> matchedSymptoms,

            @Schema(description = "Desglose de outcomes")
            Map<String, Integer> outcomeBreakdown,

            @Schema(description = "Si hay coincidencia de ubicacion")
            Boolean locationMatch
    ) {}

    @Schema(description = "Metadata del procesamiento de la recomendacion")
    public record RecommendationMetadata(
            @Schema(description = "Total de casos historicos buscados", example = "150")
            Integer totalCasesSearched,

            @Schema(description = "Total de matches encontrados", example = "15")
            Integer totalMatchesFound,

            @Schema(description = "Duracion del procesamiento en ms", example = "5800")
            Long processingDurationMs,

            @Schema(description = "Proveedor LLM", example = "CLAUDE")
            String llmProvider,

            @Schema(description = "Modelo LLM", example = "claude-sonnet-4-20250514")
            String llmModel,

            @Schema(description = "Version del prompt", example = "v1.0")
            String promptVersion
    ) {}
}
