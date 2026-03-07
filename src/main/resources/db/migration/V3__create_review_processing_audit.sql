CREATE TABLE review_processing_audit (
    id                     BIGSERIAL PRIMARY KEY,
    review_id              UUID NOT NULL REFERENCES clinical_reviews(id),
    correlation_id         UUID NOT NULL,
    event_type             VARCHAR(50) NOT NULL,
    from_status            VARCHAR(30),
    to_status              VARCHAR(30),
    prompt_version         VARCHAR(20),
    prompt_text            TEXT,
    llm_raw_response       TEXT,
    error_code             VARCHAR(50),
    error_message          TEXT,
    details_json           JSONB,
    attempt_number         INTEGER DEFAULT 1,
    duration_ms            INTEGER,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_review_id ON review_processing_audit(review_id);
CREATE INDEX idx_audit_correlation ON review_processing_audit(correlation_id);
CREATE INDEX idx_audit_event_type ON review_processing_audit(event_type);
CREATE INDEX idx_audit_created ON review_processing_audit(created_at);
