package com.vetplatform.reviewextractor.controller;

import com.vetplatform.reviewextractor.application.service.ClinicalReviewService;
import com.vetplatform.reviewextractor.dto.request.CreateClinicalReviewRequest;
import com.vetplatform.reviewextractor.dto.request.ReprocessReviewRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinical-reviews")
@Tag(name = "Clinical Reviews", description = "Endpoints para la extraccion y estructuracion de resenas clinicas veterinarias")
public class ClinicalReviewController {

    private final ClinicalReviewService reviewService;

    public ClinicalReviewController(ClinicalReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "Crear y procesar una resena clinica",
            description = """
                    Recibe una resena clinica en texto libre escrita por un dueno de mascota,
                    la persiste y lanza su procesamiento asincrono mediante un LLM.

                    El procesamiento incluye: extraccion de datos clinicos, validacion de schema,
                    normalizacion a vocabulario controlado y persistencia del resultado estructurado.

                    La respuesta es inmediata (202 Accepted) con un `reviewId` para consultar el estado y resultado."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Resena aceptada, procesamiento iniciado",
                    content = @Content(schema = @Schema(implementation = CreateClinicalReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error de validacion: texto vacio, muy corto/largo o locale no soportado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "Error de validacion en el request",
                                      "timestamp": "2026-03-06T14:30:00Z",
                                      "fieldErrors": [
                                        {
                                          "field": "reviewText",
                                          "rejectedValue": "",
                                          "message": "El campo 'reviewText' no puede estar vacio"
                                        }
                                      ]
                                    }"""))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit excedido para el petOwnerId",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<CreateClinicalReviewResponse> createReview(
            @Valid @RequestBody CreateClinicalReviewRequest request
    ) {
        CreateClinicalReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "Obtener resultado estructurado de una resena",
            description = """
                    Retorna el resultado completo de la extraccion clinica, incluyendo:
                    especie, sintomas normalizados, procedimientos, veterinario, ubicacion,
                    outcome, confianza y metadata del procesamiento (modelo LLM, version del prompt, tokens).

                    Si la resena aun esta en procesamiento, retorna 409 Conflict."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado estructurado disponible",
                    content = @Content(schema = @Schema(implementation = StructuredClinicalReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Review no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resena aun en procesamiento",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": "PROCESSING_IN_PROGRESS",
                              "message": "La resena aun se esta procesando. Consulte el endpoint de status.",
                              "reviewId": "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c",
                              "currentStatus": "PROCESSING",
                              "_links": {
                                "status": "/api/v1/clinical-reviews/b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c/status"
                              }
                            }"""))
            )
    })
    @GetMapping("/{reviewId}/result")
    public ResponseEntity<?> getResult(
            @Parameter(description = "UUID de la resena clinica", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
            @PathVariable UUID reviewId
    ) {
        StructuredClinicalReviewResponse result = reviewService.getResult(reviewId);

        if (result == null) {
            ReviewProcessingStatusResponse status = reviewService.getStatus(reviewId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "PROCESSING_IN_PROGRESS",
                    "message", "La resena aun se esta procesando. Consulte el endpoint de status.",
                    "reviewId", reviewId,
                    "currentStatus", status.status(),
                    "_links", Map.of(
                            "status", "/api/v1/clinical-reviews/" + reviewId + "/status"
                    )
            ));
        }

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Consultar estado de procesamiento",
            description = """
                    Retorna el estado actual del procesamiento de una resena.
                    Disenado para polling. Incluye el paso actual, pasos completados,
                    cantidad de reintentos y, si fallo, la razon y si es retriable."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado retornado exitosamente",
                    content = @Content(schema = @Schema(implementation = ReviewProcessingStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Review no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{reviewId}/status")
    public ResponseEntity<ReviewProcessingStatusResponse> getStatus(
            @Parameter(description = "UUID de la resena clinica", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
            @PathVariable UUID reviewId
    ) {
        return ResponseEntity.ok(reviewService.getStatus(reviewId));
    }

    @Operation(
            summary = "Reprocesar una resena clinica",
            description = """
                    Relanza el procesamiento de una resena fallida o fuerza el reprocesamiento
                    de una completada (por ejemplo, tras actualizar la version del prompt).

                    Para reprocesar una resena en estado COMPLETED se requiere `forceReprocess: true`.
                    No se puede reprocesar una resena que ya esta en estado PROCESSING."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Reprocesamiento aceptado",
                    content = @Content(schema = @Schema(implementation = ReprocessReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Review no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resena ya esta en procesamiento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Resena completada y forceReprocess es false",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "REPROCESS_NOT_ALLOWED",
                                      "message": "Review completado. Use forceReprocess=true para forzar.",
                                      "timestamp": "2026-03-06T15:00:00Z",
                                      "fieldErrors": null
                                    }"""))
            )
    })
    @PostMapping("/{reviewId}/reprocess")
    public ResponseEntity<ReprocessReviewResponse> reprocess(
            @Parameter(description = "UUID de la resena clinica", example = "b7e2c4a1-9f3d-4e5b-8a1c-6d7e8f9a0b1c")
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReprocessReviewRequest request
    ) {
        ReprocessReviewResponse response = reviewService.reprocess(reviewId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
