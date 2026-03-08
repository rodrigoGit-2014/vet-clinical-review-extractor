package com.vetplatform.reviewextractor.controller;

import com.vetplatform.reviewextractor.application.service.VetRecommendationService;
import com.vetplatform.reviewextractor.dto.request.CreateRecommendationRequest;
import com.vetplatform.reviewextractor.dto.request.RecalculateRecommendationRequest;
import com.vetplatform.reviewextractor.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vet-recommendations")
@Tag(name = "Vet Recommendations", description = "Endpoints para recomendacion de veterinarios basada en casos clinicos similares")
public class VetRecommendationController {

    private final VetRecommendationService recommendationService;

    public VetRecommendationController(VetRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(
            summary = "Crear solicitud de recomendacion",
            description = """
                    Recibe texto libre del dueno describiendo el problema de su mascota,
                    interpreta los sintomas mediante un LLM, busca casos clinicos similares
                    en el historial y genera un ranking de veterinarios recomendados.

                    La respuesta es inmediata (202 Accepted) con un `requestId` para consultar el estado y resultado."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Solicitud aceptada, procesamiento iniciado",
                    content = @Content(schema = @Schema(implementation = CreateRecommendationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error de validacion: texto vacio, muy corto/largo o locale no soportado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "Error de validacion en el request",
                                      "timestamp": "2026-03-07T14:30:00Z",
                                      "fieldErrors": [
                                        {
                                          "field": "clientText",
                                          "rejectedValue": "",
                                          "message": "El campo 'clientText' no puede estar vacio"
                                        }
                                      ]
                                    }"""))
            )
    })
    @PostMapping
    public ResponseEntity<CreateRecommendationResponse> createRecommendation(
            @Valid @RequestBody CreateRecommendationRequest request
    ) {
        CreateRecommendationResponse response = recommendationService.createRecommendation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "Obtener resultado de recomendacion",
            description = """
                    Retorna el ranking de veterinarios recomendados con explicaciones,
                    la interpretacion estructurada de la solicitud del cliente y
                    metadata del procesamiento.

                    Si la solicitud aun esta en procesamiento, retorna 409 Conflict."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado disponible",
                    content = @Content(schema = @Schema(implementation = RecommendationResultResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Solicitud aun en procesamiento",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": "PROCESSING_IN_PROGRESS",
                              "message": "La recomendacion aun se esta procesando.",
                              "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                              "currentStatus": "INTERPRETING",
                              "_links": {
                                "status": "/api/v1/vet-recommendations/a1b2c3d4-e5f6-7890-abcd-ef1234567890/status"
                              }
                            }"""))
            )
    })
    @GetMapping("/{requestId}/result")
    public ResponseEntity<?> getResult(
            @Parameter(description = "UUID de la solicitud", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID requestId
    ) {
        RecommendationResultResponse result = recommendationService.getResult(requestId);

        if (result == null) {
            RecommendationStatusResponse status = recommendationService.getStatus(requestId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "PROCESSING_IN_PROGRESS",
                    "message", "La recomendacion aun se esta procesando.",
                    "requestId", requestId,
                    "currentStatus", status.status(),
                    "_links", Map.of(
                            "status", "/api/v1/vet-recommendations/" + requestId + "/status"
                    )
            ));
        }

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Consultar estado de procesamiento",
            description = """
                    Retorna el estado actual del procesamiento de una solicitud de recomendacion.
                    Disenado para polling. Incluye el paso actual, pasos completados,
                    cantidad de reintentos y, si fallo, la razon y si es retriable."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado retornado exitosamente",
                    content = @Content(schema = @Schema(implementation = RecommendationStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{requestId}/status")
    public ResponseEntity<RecommendationStatusResponse> getStatus(
            @Parameter(description = "UUID de la solicitud", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(recommendationService.getStatus(requestId));
    }

    @Operation(
            summary = "Recalcular recomendacion",
            description = """
                    Relanza el procesamiento de una solicitud fallida o fuerza el recalculo
                    de una completada (por ejemplo, tras actualizar resenas historicas o
                    la version del prompt).

                    Para recalcular una solicitud en estado COMPLETED se requiere `forceRecalculate: true`.
                    No se puede recalcular una solicitud que ya esta en procesamiento."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Recalculo aceptado",
                    content = @Content(schema = @Schema(implementation = RecalculateRecommendationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Solicitud ya esta en procesamiento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Solicitud completada y forceRecalculate es false",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "RECALCULATE_NOT_ALLOWED",
                                      "message": "Recomendacion completada. Use forceRecalculate=true para forzar.",
                                      "timestamp": "2026-03-07T15:00:00Z",
                                      "fieldErrors": null
                                    }"""))
            )
    })
    @PostMapping("/{requestId}/recalculate")
    public ResponseEntity<RecalculateRecommendationResponse> recalculate(
            @Parameter(description = "UUID de la solicitud", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID requestId,
            @Valid @RequestBody RecalculateRecommendationRequest request
    ) {
        RecalculateRecommendationResponse response = recommendationService.recalculate(requestId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
