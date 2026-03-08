CREATE TABLE clinical_reviews (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_text               TEXT NOT NULL,
    pet_owner_id           VARCHAR(100) NOT NULL,
    locale                 VARCHAR(10) NOT NULL DEFAULT 'es-CL',
    status                 VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    prompt_version         VARCHAR(20),
    llm_provider           VARCHAR(30),
    llm_model              VARCHAR(100),
    extracted_json         JSONB,
    normalized_json        JSONB,
    overall_confidence     DECIMAL(3,2),
    processing_duration_ms INTEGER,
    retry_count            INTEGER DEFAULT 0,
    reprocess_count        INTEGER DEFAULT 0,
    failure_reason         VARCHAR(50),
    failure_message        TEXT,
    input_tokens           INTEGER,
    output_tokens          INTEGER,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clinical_reviews_status ON clinical_reviews(status);
CREATE INDEX idx_clinical_reviews_pet_owner ON clinical_reviews(pet_owner_id);
CREATE INDEX idx_clinical_reviews_created ON clinical_reviews(created_at);
CREATE INDEX idx_clinical_reviews_confidence ON clinical_reviews(overall_confidence);
