-- =============================================================================
-- V8: Seed 10 clinical reviews for testing the vet recommendation flow
-- =============================================================================
-- Scenario:
--   DOGS (6):
--     4 reviews → Dr. Carlos Mendoza (Clinica Patitas Felices) — fracturas
--     1 review  → Dra. Ana Gutierrez (Pet Spa Santiago) — corte de unas
--     1 review  → Dr. Pedro Soto (Salon Canino Estrella) — aseo/grooming
--   CATS (4):
--     2 reviews → Dra. Maria Fernandez (Centro Oftalmologico Veterinario) — operaciones de ojo
--     1 review  → Dr. Luis Ramirez (Clinica Felina Central) — problema dental
--     1 review  → Dra. Patricia Mora (Hospital Veterinario Sur) — dermatologia
-- =============================================================================

-- =====================
-- DOGS — FRACTURAS (4)
-- =====================

-- Dog 1: Fractura de pata — Dr. Carlos Mendoza
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0001-4000-8000-000000000001',
    'Lleve a mi perro Toby al veterinario porque se cayo de la escalera y no podia apoyar la pata trasera derecha. El Dr. Carlos Mendoza de la Clinica Patitas Felices le tomo radiografias y le diagnostico fractura de tibia. Le pusieron un yeso y en 6 semanas quedo como nuevo.',
    'owner-test-001', 'es-CL', 'COMPLETED', 'v1.0', 0.92,
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0001-4000-8000-000000000001',
    'a0000001-0001-4000-8000-000000000001',
    'DOG', 'Mestizo', 'Toby',
    '[{"description": "no puede apoyar la pata trasera derecha", "suggested_code": "LAMENESS", "normalized_code": "LAMENESS", "body_area": "LIMBS", "severity": "SEVERE"}]',
    '[{"description": "radiografia", "suggested_code": "DIAGNOSTIC_IMAGING", "type": "DIAGNOSTIC"}, {"description": "yeso por fractura de tibia", "suggested_code": "FRACTURE_TREATMENT", "type": "SURGICAL"}]',
    '[{"name": "Meloxicam", "purpose": "antiinflamatorio"}]',
    'Dr. Carlos Mendoza', 'Clinica Patitas Felices',
    'Santiago, Chile', 'Metropolitana', 'CL',
    'FULLY_RECOVERED', 'Recuperacion completa en 6 semanas con yeso',
    0.92, NOW() - INTERVAL '30 days'
);

-- Dog 2: Fractura de cadera — Dr. Carlos Mendoza
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0002-4000-8000-000000000002',
    'Mi perra Luna fue atropellada por una bicicleta y quedo renqueando mucho. La lleve donde el Dr. Carlos Mendoza en Patitas Felices y le diagnostico fractura de cadera. La opero y le puso una placa. Lleva 3 semanas y ya camina mejor pero todavia cojea un poco.',
    'owner-test-002', 'es-CL', 'COMPLETED', 'v1.0', 0.88,
    NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0002-4000-8000-000000000002',
    'a0000001-0002-4000-8000-000000000002',
    'DOG', NULL, 'Luna',
    '[{"description": "renquea mucho, cojea", "suggested_code": "LAMENESS", "normalized_code": "LAMENESS", "body_area": "LIMBS", "severity": "SEVERE"}, {"description": "dolor al caminar", "suggested_code": "PAIN", "normalized_code": "PAIN", "body_area": "LIMBS", "severity": "MODERATE"}]',
    '[{"description": "radiografia de cadera", "suggested_code": "DIAGNOSTIC_IMAGING", "type": "DIAGNOSTIC"}, {"description": "cirugia con placa por fractura de cadera", "suggested_code": "FRACTURE_TREATMENT", "type": "SURGICAL"}]',
    '[{"name": "Tramadol", "purpose": "analgesico"}, {"name": "Cefalexina", "purpose": "antibiotico"}]',
    'Dr. Carlos Mendoza', 'Clinica Patitas Felices',
    'Providencia, Santiago', 'Metropolitana', 'CL',
    'IMPROVING', 'En recuperacion, ya camina pero todavia cojea un poco',
    0.88, NOW() - INTERVAL '25 days'
);

-- Dog 3: Fractura de patita delantera — Dr. Carlos Mendoza
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0003-4000-8000-000000000003',
    'Mi cachorro Max se cayo del sofa y empezo a llorar mucho y a no mover la patita delantera izquierda. El Dr. Carlos Mendoza nos atendio de urgencia en la Clinica Patitas Felices. Era una fractura del radio. Le puso ferula y nos dio analgesicos. A las 4 semanas ya estaba corriendo.',
    'owner-test-003', 'es-CL', 'COMPLETED', 'v1.0', 0.90,
    NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0003-4000-8000-000000000003',
    'a0000001-0003-4000-8000-000000000003',
    'DOG', 'Labrador', 'Max',
    '[{"description": "no mueve la patita delantera izquierda, llora de dolor", "suggested_code": "LAMENESS", "normalized_code": "LAMENESS", "body_area": "LIMBS", "severity": "SEVERE"}, {"description": "dolor agudo", "suggested_code": "PAIN", "normalized_code": "PAIN", "body_area": "LIMBS", "severity": "SEVERE"}]',
    '[{"description": "ferula por fractura de radio", "suggested_code": "FRACTURE_TREATMENT", "type": "SURGICAL"}]',
    '[{"name": "Meloxicam", "purpose": "analgesico antiinflamatorio"}]',
    'Dr. Carlos Mendoza', 'Clinica Patitas Felices',
    'Las Condes, Santiago', 'Metropolitana', 'CL',
    'FULLY_RECOVERED', 'Recuperacion total en 4 semanas con ferula',
    0.90, NOW() - INTERVAL '20 days'
);

-- Dog 4: Fractura de costilla — Dr. Carlos Mendoza
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0004-4000-8000-000000000004',
    'Mi perro Rocky peleo con otro perro grande y quedo muy adolorido en el torax. No se dejaba tocar. Lo lleve al Dr. Carlos Mendoza que trabaja en la Clinica Patitas Felices y tras hacerle radiografia le encontro una fractura de costilla. Le dio reposo estricto y medicamentos. Esta estable pero se recupera lento.',
    'owner-test-004', 'es-CL', 'COMPLETED', 'v1.0', 0.85,
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0004-4000-8000-000000000004',
    'a0000001-0004-4000-8000-000000000004',
    'DOG', 'Pitbull', 'Rocky',
    '[{"description": "dolor intenso en el torax, no se deja tocar", "suggested_code": "PAIN", "normalized_code": "PAIN", "body_area": "THORAX", "severity": "SEVERE"}, {"description": "inflamacion en zona costal", "suggested_code": "INFLAMMATION", "normalized_code": "INFLAMMATION", "body_area": "THORAX", "severity": "MODERATE"}]',
    '[{"description": "radiografia de torax", "suggested_code": "DIAGNOSTIC_IMAGING", "type": "DIAGNOSTIC"}, {"description": "tratamiento conservador fractura de costilla", "suggested_code": "FRACTURE_TREATMENT", "type": "MEDICAL"}]',
    '[{"name": "Tramadol", "purpose": "analgesico"}, {"name": "Meloxicam", "purpose": "antiinflamatorio"}]',
    'Dr. Carlos Mendoza', 'Clinica Patitas Felices',
    'Nunoa, Santiago', 'Metropolitana', 'CL',
    'STABLE', 'Estable, en reposo estricto. Recuperacion lenta',
    0.85, NOW() - INTERVAL '15 days'
);

-- ============================
-- DOGS — CORTE DE UNAS (1)
-- ============================

-- Dog 5: Corte de unas — Dra. Ana Gutierrez
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0005-4000-8000-000000000005',
    'Lleve a mi perrita Lola a que le cortaran las unas porque las tenia muy largas y se le estaban curvando. La Dra. Ana Gutierrez del Pet Spa Santiago le hizo un corte profesional y le enseno a mi hijo como mantenerlas. Lola quedo feliz caminando mejor.',
    'owner-test-005', 'es-CL', 'COMPLETED', 'v1.0', 0.80,
    NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0005-4000-8000-000000000005',
    'a0000001-0005-4000-8000-000000000005',
    'DOG', 'Poodle', 'Lola',
    '[{"description": "unas muy largas y curvadas, dificultad al caminar", "suggested_code": "LAMENESS", "normalized_code": "LAMENESS", "body_area": "LIMBS", "severity": "MILD"}]',
    '[{"description": "corte profesional de unas", "suggested_code": "NAIL_TRIMMING", "type": "PREVENTIVE"}]',
    '[]',
    'Dra. Ana Gutierrez', 'Pet Spa Santiago',
    'Vitacura, Santiago', 'Metropolitana', 'CL',
    'FULLY_RECOVERED', 'Unas cortadas correctamente, camina mejor',
    0.80, NOW() - INTERVAL '10 days'
);

-- ============================
-- DOGS — ASEO/GROOMING (1)
-- ============================

-- Dog 6: Bano y grooming — Dr. Pedro Soto
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0006-4000-8000-000000000006',
    'Lleve a mi perro Coco al Salon Canino Estrella con el Dr. Pedro Soto para un bano completo y corte de pelo porque estaba muy enredado y con mal olor. Le hicieron un grooming profesional, le limpiaron las orejas y le revisaron la piel. Quedo como perro de exposicion.',
    'owner-test-006', 'es-CL', 'COMPLETED', 'v1.0', 0.78,
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0006-4000-8000-000000000006',
    'a0000001-0006-4000-8000-000000000006',
    'DOG', 'Cocker Spaniel', 'Coco',
    '[{"description": "pelo enredado y mal olor", "suggested_code": "PRURITUS", "normalized_code": "PRURITUS", "body_area": "SKIN", "severity": "MILD"}]',
    '[{"description": "bano completo y grooming profesional", "suggested_code": "GROOMING", "type": "PREVENTIVE"}, {"description": "limpieza de oidos", "suggested_code": "EAR_CLEANING", "type": "PREVENTIVE"}]',
    '[]',
    'Dr. Pedro Soto', 'Salon Canino Estrella',
    'La Reina, Santiago', 'Metropolitana', 'CL',
    'FULLY_RECOVERED', 'Grooming completo exitoso',
    0.78, NOW() - INTERVAL '8 days'
);

-- ====================================
-- CATS — OPERACIONES DE OJO (2)
-- ====================================

-- Cat 7: Cirugia de cataratas — Dra. Maria Fernandez
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0007-4000-8000-000000000007',
    'Mi gata Michi empezo a chocarse con los muebles y tenia los ojos nublados. La Dra. Maria Fernandez del Centro Oftalmologico Veterinario le diagnostico cataratas bilaterales. La opero con una tecnica de facoemulsificacion y ahora ve perfectamente. Excelente doctora especialista en ojos.',
    'owner-test-007', 'es-CL', 'COMPLETED', 'v1.0', 0.94,
    NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0007-4000-8000-000000000007',
    'a0000001-0007-4000-8000-000000000007',
    'CAT', 'Siames', 'Michi',
    '[{"description": "se choca con los muebles, ojos nublados, perdida de vision", "suggested_code": "VISION_LOSS", "normalized_code": "VISION_LOSS", "body_area": "OCULAR", "severity": "SEVERE"}]',
    '[{"description": "cirugia de cataratas por facoemulsificacion", "suggested_code": "EYE_SURGERY", "type": "SURGICAL"}]',
    '[{"name": "Tobramicina oftalmica", "purpose": "antibiotico ocular"}, {"name": "Prednisolona oftalmica", "purpose": "antiinflamatorio ocular"}]',
    'Dra. Maria Fernandez', 'Centro Oftalmologico Veterinario',
    'Providencia, Santiago', 'Metropolitana', 'CL',
    'FULLY_RECOVERED', 'Vision restaurada completamente tras cirugia de cataratas',
    0.94, NOW() - INTERVAL '22 days'
);

-- Cat 8: Ulcera corneal — Dra. Maria Fernandez
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0008-4000-8000-000000000008',
    'Mi gatito Simba tenia un ojo rojo e hinchado que le lloraba mucho. Lo lleve al Centro Oftalmologico Veterinario donde la Dra. Maria Fernandez le diagnostico una ulcera corneal profunda. Le hizo una cirugia con colgajo conjuntival y le receto gotas. Va mejorando de a poco.',
    'owner-test-008', 'es-CL', 'COMPLETED', 'v1.0', 0.91,
    NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0008-4000-8000-000000000008',
    'a0000001-0008-4000-8000-000000000008',
    'CAT', 'Naranja comun', 'Simba',
    '[{"description": "ojo rojo, hinchado, lagrimeo excesivo", "suggested_code": "INFLAMMATION", "normalized_code": "INFLAMMATION", "body_area": "OCULAR", "severity": "SEVERE"}, {"description": "dolor ocular", "suggested_code": "PAIN", "normalized_code": "PAIN", "body_area": "OCULAR", "severity": "MODERATE"}]',
    '[{"description": "cirugia corneal con colgajo conjuntival", "suggested_code": "EYE_SURGERY", "type": "SURGICAL"}]',
    '[{"name": "Ofloxacino oftalmica", "purpose": "antibiotico ocular"}, {"name": "Atropina oftalmica", "purpose": "cicloplejico"}]',
    'Dra. Maria Fernandez', 'Centro Oftalmologico Veterinario',
    'Santiago Centro', 'Metropolitana', 'CL',
    'IMPROVING', 'Mejorando progresivamente tras cirugia corneal',
    0.91, NOW() - INTERVAL '12 days'
);

-- ====================================
-- CATS — ESPECIALIDADES DIVERSAS (2)
-- ====================================

-- Cat 9: Problema dental — Dr. Luis Ramirez
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0009-4000-8000-000000000009',
    'Mi gata Pelusa dejo de comer y le olia muy mal la boca. La lleve donde el Dr. Luis Ramirez en la Clinica Felina Central. Le hizo una limpieza dental bajo anestesia y le sacaron 3 muelas que estaban podridas. Ahora come sin problema y ya no le huele feo.',
    'owner-test-009', 'es-CL', 'COMPLETED', 'v1.0', 0.87,
    NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0009-4000-8000-000000000009',
    'a0000001-0009-4000-8000-000000000009',
    'CAT', 'Persa', 'Pelusa',
    '[{"description": "dejo de comer, inapetencia", "suggested_code": "APPETITE_LOSS", "normalized_code": "APPETITE_LOSS", "body_area": "ORAL", "severity": "MODERATE"}, {"description": "mal aliento, halitosis severa", "suggested_code": "HALITOSIS", "normalized_code": "HALITOSIS", "body_area": "ORAL", "severity": "MODERATE"}]',
    '[{"description": "limpieza dental bajo anestesia", "suggested_code": "DENTAL_CLEANING", "type": "SURGICAL"}, {"description": "extraccion de 3 muelas", "suggested_code": "DENTAL_EXTRACTION", "type": "SURGICAL"}]',
    '[{"name": "Amoxicilina", "purpose": "antibiotico"}, {"name": "Meloxicam", "purpose": "analgesico"}]',
    'Dr. Luis Ramirez', 'Clinica Felina Central',
    'Nunoa, Santiago', 'Metropolitana', 'CL',
    'FULLY_RECOVERED', 'Come normalmente tras extraccion dental',
    0.87, NOW() - INTERVAL '18 days'
);

-- Cat 10: Dermatitis/alergia — Dra. Patricia Mora
INSERT INTO clinical_reviews (id, raw_text, pet_owner_id, locale, status, prompt_version, overall_confidence, created_at, updated_at)
VALUES (
    'a0000001-0010-4000-8000-000000000010',
    'Mi gato Garfield se rascaba todo el dia y se le estaba cayendo el pelo en parches. Lo lleve al Hospital Veterinario Sur con la Dra. Patricia Mora que es dermatologa veterinaria. Le diagnostico dermatitis alergica y le receto un tratamiento con corticoides y dieta hipoalergenica. Ya casi no se rasca.',
    'owner-test-010', 'es-CL', 'COMPLETED', 'v1.0', 0.89,
    NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'
);

INSERT INTO structured_clinical_reviews (id, review_id, species, breed, pet_name, symptoms_json, procedures_json, medications_json, vet_name, vet_clinic, location_raw, location_region, location_country, outcome_status, outcome_description, overall_confidence, created_at)
VALUES (
    'b0000001-0010-4000-8000-000000000010',
    'a0000001-0010-4000-8000-000000000010',
    'CAT', 'Naranja comun', 'Garfield',
    '[{"description": "se rasca todo el dia, prurito intenso", "suggested_code": "PRURITUS", "normalized_code": "PRURITUS", "body_area": "SKIN", "severity": "SEVERE"}, {"description": "caida de pelo en parches, alopecia", "suggested_code": "ALOPECIA", "normalized_code": "ALOPECIA", "body_area": "SKIN", "severity": "MODERATE"}]',
    '[{"description": "examen dermatologico", "suggested_code": "DERMATOLOGY_EXAM", "type": "DIAGNOSTIC"}]',
    '[{"name": "Prednisolona", "purpose": "corticoide antiinflamatorio"}, {"name": "Dieta hipoalergenica", "purpose": "manejo nutricional"}]',
    'Dra. Patricia Mora', 'Hospital Veterinario Sur',
    'La Florida, Santiago', 'Metropolitana', 'CL',
    'IMPROVING', 'Mejora significativa con tratamiento, casi no se rasca',
    0.89, NOW() - INTERVAL '14 days'
);
