CREATE TABLE normalization_synonyms (
    id                     SERIAL PRIMARY KEY,
    category               VARCHAR(30) NOT NULL,
    raw_term               VARCHAR(200) NOT NULL,
    normalized_code        VARCHAR(100) NOT NULL,
    normalized_label       VARCHAR(200) NOT NULL,
    locale                 VARCHAR(10) DEFAULT 'es',
    active                 BOOLEAN DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_norm_category_term ON normalization_synonyms(category, raw_term, locale);
CREATE INDEX idx_norm_category ON normalization_synonyms(category);
CREATE INDEX idx_norm_code ON normalization_synonyms(normalized_code);
