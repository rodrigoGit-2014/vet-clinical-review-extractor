# Vet Clinical Review Extractor

## Project Overview
Microservice for a veterinary startup targeting Latin America. Receives free-text clinical reviews written by pet owners in Spanish, processes them through an LLM (Claude/OpenAI), and outputs structured/normalized clinical data. This feeds a downstream veterinarian recommendation engine (separate module).

## Stack
- **Language:** Java 17+ (running on Java 25)
- **Framework:** Spring Boot 3.3.5
- **Build:** Gradle 9.4.0 (`./gradlew build`)
- **Database:** PostgreSQL 16 (via Docker Compose)
- **Migrations:** Flyway (V1-V5 in `src/main/resources/db/migration/`)
- **LLM:** Claude API via WebClient (abstracted behind `LlmExtractionClient` interface)
- **API Docs:** SpringDoc OpenAPI 2.6.0 — Swagger UI at `/swagger-ui.html`
- **Tests:** JUnit 5 + Mockito (29 tests). Run with `./gradlew test`

## Architecture

### Processing Flow
```
POST /api/v1/clinical-reviews → 202 Accepted
  → Persist raw (RECEIVED)
  → @Async processing:
    PROCESSING → build prompt → invoke LLM → parse JSON
    → EXTRACTION_COMPLETED → validate → VALIDATION_PASSED
    → normalize → persist structured → COMPLETED
  → GET .../status (polling)
  → GET .../result (final structured data)
```

### Package Structure
```
com.vetplatform.reviewextractor/
  config/          — AsyncConfig, LlmClientConfig, OpenApiConfig
  controller/      — ClinicalReviewController (4 REST endpoints)
  dto/request/     — CreateClinicalReviewRequest, ReprocessReviewRequest
  dto/response/    — Create, Status, Reprocess, Structured, Error responses
  domain/entity/   — ClinicalReview, StructuredClinicalReview, ReviewProcessingAudit, NormalizationSynonym
  domain/enums/    — ReviewStatus, Species, BodyArea, ProcedureType, OutcomeStatus, AuditEventType
  domain/repository/ — JPA repositories
  application/service/ — ClinicalReviewService (orchestrator), ExtractionValidatorService, ClinicalNormalizationService, ProcessingAuditService
  infrastructure/llm/  — LlmExtractionClient (interface), ClaudeLlmClient, PromptBuilderService, LlmResponseParser
  infrastructure/exception/ — Custom exceptions
  infrastructure/handler/   — GlobalExceptionHandler
```

### Key Design Decisions
- **Async processing (202 + polling):** LLM calls take 3-10s; avoids HTTP timeouts
- **Separated extraction & normalization:** LLM extracts raw; normalization uses PostgreSQL synonym tables (`normalization_synonyms`) with fuzzy matching
- **Both `extracted_json` and `normalized_json` persisted:** Enables reprocesamiento without re-invoking LLM
- **Versioned prompts:** Templates in `src/main/resources/prompts/v1.0/` (system.txt, user.txt)
- **Full audit trail:** Every pipeline step logged in `review_processing_audit` with correlation IDs

## REST Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/clinical-reviews` | Submit review for processing (202) |
| GET | `/api/v1/clinical-reviews/{id}/status` | Polling processing status |
| GET | `/api/v1/clinical-reviews/{id}/result` | Get structured result |
| POST | `/api/v1/clinical-reviews/{id}/reprocess` | Reprocess a review |

## Database Tables
- `clinical_reviews` — Raw reviews + processing state + LLM metadata
- `structured_clinical_reviews` — Normalized extracted data
- `review_processing_audit` — Full event log per review
- `normalization_synonyms` — Controlled vocabulary (species, symptoms, body areas, procedures)

## Running Locally
```bash
docker compose up -d          # Start PostgreSQL
./gradlew bootRun             # Start app (Flyway runs migrations automatically)
```

## Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | vetplatform | PostgreSQL username |
| `DB_PASSWORD` | vetplatform | PostgreSQL password |
| `LLM_PROVIDER` | claude | LLM provider (claude/openai) |
| `LLM_API_KEY` | (empty) | API key for the LLM provider |
| `LLM_MODEL` | claude-sonnet-4-20250514 | Model identifier |

## Known Compatibility Notes
- **Java 25 + ByteBuddy:** Tests require `-Dnet.bytebuddy.experimental=true` (configured in build.gradle)
- **springdoc 2.6.0:** Required for Spring Boot 3.3.5 compatibility (2.7.0 requires Spring Boot 3.4+)
- **Gradle 9.4.0:** Required for Java 25 support (Gradle 8.x Groovy compiler doesn't support class file version 69)
- **Controller tests:** Use standalone MockMvc with `@ExtendWith(MockitoExtension.class)` instead of `@WebMvcTest` to avoid loading full app context

## Blueprint
Full 20-section technical blueprint available at:
`.claude/plans/fluttering-splashing-micali.md`
