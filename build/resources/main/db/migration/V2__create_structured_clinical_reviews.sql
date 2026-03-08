CREATE TABLE structured_clinical_reviews (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id              UUID NOT NULL REFERENCES clinical_reviews(id),
    species                VARCHAR(30),
    breed                  VARCHAR(100),
    pet_name               VARCHAR(100),
    symptoms_json          JSONB NOT NULL DEFAULT '[]',
    procedures_json        JSONB NOT NULL DEFAULT '[]',
    medications_json       JSONB NOT NULL DEFAULT '[]',
    vet_name               VARCHAR(200),
    vet_clinic             VARCHAR(200),
    location_raw           VARCHAR(200),
    location_normalized    VARCHAR(200),
    location_region        VARCHAR(100),
    location_country       VARCHAR(5),
    outcome_status         VARCHAR(30),
    outcome_description    TEXT,
    overall_confidence     DECIMAL(3,2),
    extraction_notes       TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_structured_review_id ON structured_clinical_reviews(review_id);
CREATE INDEX idx_structured_species ON structured_clinical_reviews(species);
CREATE INDEX idx_structured_vet_name ON structured_clinical_reviews(vet_name);
CREATE INDEX idx_structured_location ON structured_clinical_reviews(location_country, location_region);
CREATE INDEX idx_structured_outcome ON structured_clinical_reviews(outcome_status);
