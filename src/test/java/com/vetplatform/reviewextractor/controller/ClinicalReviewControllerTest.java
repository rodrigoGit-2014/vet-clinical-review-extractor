package com.vetplatform.reviewextractor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.ClinicalReviewService;
import com.vetplatform.reviewextractor.dto.response.CreateClinicalReviewResponse;
import com.vetplatform.reviewextractor.dto.response.ReviewProcessingStatusResponse;
import com.vetplatform.reviewextractor.infrastructure.exception.ReviewNotFoundException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClinicalReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClinicalReviewService reviewService;

    @InjectMocks
    private ClinicalReviewController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn202WhenCreatingReview() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.createReview(any())).thenReturn(
                new CreateClinicalReviewResponse(
                        reviewId,
                        "RECEIVED",
                        Instant.now(),
                        Map.of("self", "/api/v1/clinical-reviews/" + reviewId)
                )
        );

        String requestBody = """
                {
                  "reviewText": "Mi perro tenia un bulto en la zona anal que empezo a sangrar.",
                  "petOwnerId": "owner-123",
                  "locale": "es-CL"
                }
                """;

        mockMvc.perform(post("/api/v1/clinical-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void shouldReturn400WhenReviewTextEmpty() throws Exception {
        String requestBody = """
                {
                  "reviewText": "",
                  "petOwnerId": "owner-123",
                  "locale": "es-CL"
                }
                """;

        mockMvc.perform(post("/api/v1/clinical-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenReviewTextTooShort() throws Exception {
        String requestBody = """
                {
                  "reviewText": "corto",
                  "petOwnerId": "owner-123",
                  "locale": "es-CL"
                }
                """;

        mockMvc.perform(post("/api/v1/clinical-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenGettingStatus() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.getStatus(reviewId)).thenReturn(
                new ReviewProcessingStatusResponse(
                        reviewId, "PROCESSING", Instant.now(), Instant.now(),
                        "LLM_INVOCATION", List.of("RECEIVED"), 0,
                        null, null, false
                )
        );

        mockMvc.perform(get("/api/v1/clinical-reviews/" + reviewId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void shouldReturn404WhenReviewNotFound() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.getStatus(reviewId)).thenThrow(new ReviewNotFoundException(reviewId));

        mockMvc.perform(get("/api/v1/clinical-reviews/" + reviewId + "/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenResultNotReady() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.getResult(reviewId)).thenReturn(null);
        when(reviewService.getStatus(reviewId)).thenReturn(
                new ReviewProcessingStatusResponse(
                        reviewId, "PROCESSING", Instant.now(), Instant.now(),
                        "LLM_INVOCATION", List.of("RECEIVED"), 0,
                        null, null, false
                )
        );

        mockMvc.perform(get("/api/v1/clinical-reviews/" + reviewId + "/result"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PROCESSING_IN_PROGRESS"));
    }
}
