CREATE TABLE vet_recommendation_requests (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_text                 TEXT NOT NULL,
    pet_owner_id                VARCHAR(100) NOT NULL,
    locale                      VARCHAR(10) NOT NULL DEFAULT 'es-CL',
    location_hint               VARCHAR(200),
    max_results                 INTEGER NOT NULL DEFAULT 5,
    status                      VARCHAR(40) NOT NULL DEFAULT 'RECEIVED',
    prompt_version              VARCHAR(20),
    llm_provider                VARCHAR(30),
    llm_model                   VARCHAR(100),
    interpreted_json            JSONB,
    normalized_interpretation   JSONB,
    interpretation_confidence   DECIMAL(3,2),
    processing_duration_ms      INTEGER,
    retry_count                 INTEGER DEFAULT 0,
    recalculate_count           INTEGER DEFAULT 0,
    failure_reason              VARCHAR(50),
    failure_message             TEXT,
    input_tokens                INTEGER,
    output_tokens               INTEGER,
    total_cases_searched        INTEGER,
    total_matches_found         INTEGER,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rec_requests_status ON vet_recommendation_requests(status);
CREATE INDEX idx_rec_requests_pet_owner ON vet_recommendation_requests(pet_owner_id);
CREATE INDEX idx_rec_requests_created ON vet_recommendation_requests(created_at);

CREATE TABLE vet_recommendations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id              UUID NOT NULL REFERENCES vet_recommendation_requests(id),
    rank_position           INTEGER NOT NULL,
    vet_name                VARCHAR(200) NOT NULL,
    vet_clinic              VARCHAR(200),
    location_country        VARCHAR(5),
    location_region         VARCHAR(100),
    score                   DECIMAL(4,3) NOT NULL,
    similar_cases_count     INTEGER NOT NULL,
    positive_outcome_rate   DECIMAL(4,3),
    avg_similarity          DECIMAL(4,3),
    location_match          BOOLEAN DEFAULT FALSE,
    explanation_json        JSONB NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recommendations_request ON vet_recommendations(request_id);
CREATE INDEX idx_recommendations_vet ON vet_recommendations(vet_name);
CREATE INDEX idx_recommendations_rank ON vet_recommendations(request_id, rank_position);

CREATE TABLE recommendation_matches (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id              UUID NOT NULL REFERENCES vet_recommendation_requests(id),
    structured_review_id    UUID NOT NULL REFERENCES structured_clinical_reviews(id),
    vet_name                VARCHAR(200) NOT NULL,
    similarity_score        DECIMAL(4,3) NOT NULL,
    species_match           BOOLEAN NOT NULL,
    symptom_overlap_codes   JSONB NOT NULL DEFAULT '[]',
    symptom_jaccard         DECIMAL(4,3),
    location_score          DECIMAL(4,3),
    outcome_status          VARCHAR(30),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rec_matches_request ON recommendation_matches(request_id);
CREATE INDEX idx_rec_matches_vet ON recommendation_matches(request_id, vet_name);
