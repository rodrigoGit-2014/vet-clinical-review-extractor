-- Drop the FK constraint on review_processing_audit.review_id
-- so it can store IDs from both clinical_reviews and vet_recommendation_requests
ALTER TABLE review_processing_audit
    DROP CONSTRAINT review_processing_audit_review_id_fkey;
