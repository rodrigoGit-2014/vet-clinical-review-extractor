package com.vetplatform.reviewextractor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.VetRecommendationService;
import com.vetplatform.reviewextractor.dto.response.CreateRecommendationResponse;
import com.vetplatform.reviewextractor.dto.response.RecalculateRecommendationResponse;
import com.vetplatform.reviewextractor.dto.response.RecommendationStatusResponse;
import com.vetplatform.reviewextractor.infrastructure.exception.RecommendationAlreadyProcessingException;
import com.vetplatform.reviewextractor.infrastructure.exception.RecommendationNotFoundException;
import com.vetplatform.reviewextractor.infrastructure.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VetRecommendationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VetRecommendationService recommendationService;

    @InjectMocks
    private VetRecommendationController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn202WhenCreatingRecommendation() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(recommendationService.createRecommendation(any())).thenReturn(
                new CreateRecommendationResponse(
                        requestId,
                        "RECEIVED",
                        Instant.now(),
                        Map.of("self", "/api/v1/vet-recommendations/" + requestId)
                )
        );

        String requestBody = """
                {
                  "clientText": "tengo mi perrito que hace dias que no come, solamente toma agua",
                  "petOwnerId": "owner-abc-123",
                  "locale": "es-CL",
                  "locationHint": "Santiago, Chile",
                  "maxResults": 5
                }
                """;

        mockMvc.perform(post("/api/v1/vet-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void shouldReturn400WhenClientTextEmpty() throws Exception {
        String requestBody = """
                {
                  "clientText": "",
                  "petOwnerId": "owner-abc-123",
                  "locale": "es-CL"
                }
                """;

        mockMvc.perform(post("/api/v1/vet-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenClientTextTooShort() throws Exception {
        String requestBody = """
                {
                  "clientText": "corto",
                  "petOwnerId": "owner-abc-123",
                  "locale": "es-CL"
                }
                """;

        mockMvc.perform(post("/api/v1/vet-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenGettingStatus() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(recommendationService.getStatus(requestId)).thenReturn(
                new RecommendationStatusResponse(
                        requestId, "INTERPRETING", Instant.now(), Instant.now(),
                        "INTERPRETATION", List.of("RECEIVED"), 0,
                        null, null, false
                )
        );

        mockMvc.perform(get("/api/v1/vet-recommendations/" + requestId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERPRETING"));
    }

    @Test
    void shouldReturn404WhenRecommendationNotFound() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(recommendationService.getStatus(requestId)).thenThrow(new RecommendationNotFoundException(requestId));

        mockMvc.perform(get("/api/v1/vet-recommendations/" + requestId + "/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenResultNotReady() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(recommendationService.getResult(requestId)).thenReturn(null);
        when(recommendationService.getStatus(requestId)).thenReturn(
                new RecommendationStatusResponse(
                        requestId, "INTERPRETING", Instant.now(), Instant.now(),
                        "INTERPRETATION", List.of("RECEIVED"), 0,
                        null, null, false
                )
        );

        mockMvc.perform(get("/api/v1/vet-recommendations/" + requestId + "/result"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PROCESSING_IN_PROGRESS"));
    }

    @Test
    void shouldReturn202WhenRecalculating() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(recommendationService.recalculate(eq(requestId), any())).thenReturn(
                new RecalculateRecommendationResponse(
                        requestId,
                        "FAILED",
                        "REPROCESSING",
                        "Se actualizaron resenas historicas",
                        1,
                        Instant.now()
                )
        );

        String requestBody = """
                {
                  "reason": "Se actualizaron resenas historicas",
                  "forceRecalculate": false
                }
                """;

        mockMvc.perform(post("/api/v1/vet-recommendations/" + requestId + "/recalculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.previousStatus").value("FAILED"))
                .andExpect(jsonPath("$.newStatus").value("REPROCESSING"));
    }

    @Test
    void shouldReturn409WhenAlreadyProcessing() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(recommendationService.recalculate(eq(requestId), any()))
                .thenThrow(new RecommendationAlreadyProcessingException(requestId));

        String requestBody = """
                {
                  "reason": "Retry",
                  "forceRecalculate": false
                }
                """;

        mockMvc.perform(post("/api/v1/vet-recommendations/" + requestId + "/recalculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }
}
