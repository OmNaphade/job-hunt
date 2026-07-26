-- Deterministic demo seed data for local testing
-- Safe to run multiple times (UPSERT on id + sequence reset).
-- All seeded users use password: Pass123! (bcrypt hash copied from local admin seed).

BEGIN;

-- =========================
-- AUTH SERVICE TABLES
-- =========================
INSERT INTO users (id, email, password_hash, role, status, created_at, updated_at)
VALUES
  (9101, 'admin.ops@jobportal.local', '$2a$10$4VLWWVMvdMEMV4.weFv61uJsE0RCN.Tro0VLLez/GYLVwFGPy5Zx.', 'ADMIN', 'ACTIVE', NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
  (9102, 'recruiter.ananya@jobportal.local', '$2a$10$4VLWWVMvdMEMV4.weFv61uJsE0RCN.Tro0VLLez/GYLVwFGPy5Zx.', 'RECRUITER', 'ACTIVE', NOW() - INTERVAL '70 days', NOW() - INTERVAL '2 days'),
  (9103, 'recruiter.vikram@jobportal.local', '$2a$10$4VLWWVMvdMEMV4.weFv61uJsE0RCN.Tro0VLLez/GYLVwFGPy5Zx.', 'RECRUITER', 'ACTIVE', NOW() - INTERVAL '65 days', NOW() - INTERVAL '2 days'),
  (9104, 'candidate.neha@jobportal.local', '$2a$10$4VLWWVMvdMEMV4.weFv61uJsE0RCN.Tro0VLLez/GYLVwFGPy5Zx.', 'JOB_SEEKER', 'ACTIVE', NOW() - INTERVAL '45 days', NOW() - INTERVAL '1 day'),
  (9105, 'candidate.arjun@jobportal.local', '$2a$10$4VLWWVMvdMEMV4.weFv61uJsE0RCN.Tro0VLLez/GYLVwFGPy5Zx.', 'JOB_SEEKER', 'ACTIVE', NOW() - INTERVAL '40 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at;

INSERT INTO refresh_tokens (id, token_hash, user_id, expires_at, revoked, created_at)
VALUES
  (9201, 'seed-refresh-admin-ops-2026-01', 9101, NOW() + INTERVAL '20 days', true, NOW() - INTERVAL '20 days'),
  (9202, 'seed-refresh-recruiter-ananya-2026-01', 9102, NOW() + INTERVAL '15 days', false, NOW() - INTERVAL '15 days'),
  (9203, 'seed-refresh-recruiter-vikram-2026-01', 9103, NOW() + INTERVAL '16 days', false, NOW() - INTERVAL '16 days'),
  (9204, 'seed-refresh-candidate-neha-2026-01', 9104, NOW() + INTERVAL '10 days', false, NOW() - INTERVAL '10 days'),
  (9205, 'seed-refresh-candidate-arjun-2026-01', 9105, NOW() + INTERVAL '8 days', false, NOW() - INTERVAL '8 days')
ON CONFLICT (id) DO UPDATE
SET token_hash = EXCLUDED.token_hash,
    user_id = EXCLUDED.user_id,
    expires_at = EXCLUDED.expires_at,
    revoked = EXCLUDED.revoked,
    created_at = EXCLUDED.created_at;

INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, used, used_at, created_at)
VALUES
  (9301, 9101, 'seed-reset-admin-ops-2026-01', NOW() + INTERVAL '2 days', true, NOW() - INTERVAL '29 days', NOW() - INTERVAL '30 days'),
  (9302, 9102, 'seed-reset-recruiter-ananya-2026-01', NOW() + INTERVAL '2 days', false, NULL, NOW() - INTERVAL '12 days'),
  (9303, 9103, 'seed-reset-recruiter-vikram-2026-01', NOW() + INTERVAL '2 days', false, NULL, NOW() - INTERVAL '11 days'),
  (9304, 9104, 'seed-reset-candidate-neha-2026-01', NOW() + INTERVAL '2 days', true, NOW() - INTERVAL '6 days', NOW() - INTERVAL '7 days'),
  (9305, 9105, 'seed-reset-candidate-arjun-2026-01', NOW() + INTERVAL '2 days', false, NULL, NOW() - INTERVAL '5 days')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    token_hash = EXCLUDED.token_hash,
    expires_at = EXCLUDED.expires_at,
    used = EXCLUDED.used,
    used_at = EXCLUDED.used_at,
    created_at = EXCLUDED.created_at;

-- =========================
-- USER SERVICE TABLES
-- =========================
INSERT INTO profiles (id, user_id, headline, summary, experience_years, current_location, created_at)
VALUES
  (9401, 9101, 'Platform Operations Lead', 'Leads internal platform governance and service reliability for the hiring stack.', 9, 'Pune', NOW() - INTERVAL '85 days'),
  (9402, 9102, 'Senior Technical Recruiter', 'Focuses on backend and data hiring for product engineering teams.', 6, 'Bengaluru', NOW() - INTERVAL '68 days'),
  (9403, 9103, 'Talent Partner - Product Teams', 'Drives full-cycle hiring for frontend and platform roles.', 5, 'Mumbai', NOW() - INTERVAL '63 days'),
  (9404, 9104, 'Backend Engineer', 'Java and Spring engineer with production microservices experience.', 4, 'Hyderabad', NOW() - INTERVAL '42 days'),
  (9405, 9105, 'Frontend Engineer', 'React developer focused on design systems and performance.', 3, 'Delhi', NOW() - INTERVAL '37 days')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    headline = EXCLUDED.headline,
    summary = EXCLUDED.summary,
    experience_years = EXCLUDED.experience_years,
    current_location = EXCLUDED.current_location,
    created_at = EXCLUDED.created_at;

INSERT INTO skills (id, name)
VALUES
  (9501, 'Java'),
  (9502, 'Spring Boot'),
  (9503, 'React'),
  (9504, 'PostgreSQL'),
  (9505, 'Kafka')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO user_skills (id, user_id, skill_id)
VALUES
  (9601, 9101, 9504),
  (9602, 9102, 9502),
  (9603, 9103, 9503),
  (9604, 9104, 9501),
  (9605, 9105, 9503)
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    skill_id = EXCLUDED.skill_id;

-- =========================
-- COMPANY SERVICE TABLES
-- =========================
INSERT INTO companies (id, name, description, website, location, created_at)
VALUES
  (9701, 'AstraFin Systems', 'Fintech platform for digital lending and risk workflows.', 'https://astrafin.example', 'Bengaluru', NOW() - INTERVAL '80 days'),
  (9702, 'Nimbus Retail Cloud', 'Cloud-native retail analytics and inventory optimization products.', 'https://nimbusretail.example', 'Mumbai', NOW() - INTERVAL '78 days'),
  (9703, 'Helix HealthTech', 'Healthcare workflow automation for hospitals and diagnostic labs.', 'https://helixhealth.example', 'Pune', NOW() - INTERVAL '75 days'),
  (9704, 'Orbit Mobility Labs', 'Electric mobility software for fleet telemetry and routing.', 'https://orbitmobility.example', 'Hyderabad', NOW() - INTERVAL '72 days'),
  (9705, 'QuantaData AI', 'AI tooling for customer intelligence and support automation.', 'https://quantadata.example', 'Remote', NOW() - INTERVAL '70 days')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    website = EXCLUDED.website,
    location = EXCLUDED.location,
    created_at = EXCLUDED.created_at;

INSERT INTO recruiters (id, user_id, company_id, designation, verified)
VALUES
  (9801, 9102, 9701, 'Lead Recruiter', true),
  (9802, 9102, 9703, 'Technical Hiring Partner', true),
  (9803, 9102, 9705, 'Hiring Consultant', false),
  (9804, 9103, 9702, 'Senior Talent Partner', true),
  (9805, 9103, 9704, 'Recruitment Specialist', true)
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    company_id = EXCLUDED.company_id,
    designation = EXCLUDED.designation,
    verified = EXCLUDED.verified;

-- =========================
-- JOB SERVICE TABLES
-- =========================
INSERT INTO jobs (id, title, description, company_id, location, salary_min, salary_max, job_type, experience_required, status, created_by, created_at)
VALUES
  (9901, 'Senior Backend Engineer', 'Build resilient Java microservices, event consumers, and API integrations for fintech products.', 9701, 'Bengaluru', 1800000, 2800000, 'FULL_TIME', 4, 'OPEN', 9102, NOW() - INTERVAL '25 days'),
  (9902, 'Frontend Engineer (React)', 'Own dashboard modules, reusable UI components, and performance tuning for retail insights.', 9702, 'Mumbai', 1400000, 2200000, 'FULL_TIME', 3, 'OPEN', 9103, NOW() - INTERVAL '22 days'),
  (9903, 'Data Platform Engineer', 'Develop data ingestion, warehouse ETL jobs, and reliability tooling in a healthcare domain.', 9703, 'Pune', 1700000, 2600000, 'FULL_TIME', 4, 'OPEN', 9102, NOW() - INTERVAL '20 days'),
  (9904, 'Platform SRE', 'Improve observability, release automation, and incident response for mobility cloud services.', 9704, 'Hyderabad', 2000000, 3000000, 'FULL_TIME', 5, 'OPEN', 9103, NOW() - INTERVAL '18 days'),
  (9905, 'Full Stack Engineer', 'Ship customer-facing workflows across React frontend and Spring backend for AI products.', 9705, 'Remote', 1600000, 2500000, 'FULL_TIME', 3, 'OPEN', 9102, NOW() - INTERVAL '15 days')
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    company_id = EXCLUDED.company_id,
    location = EXCLUDED.location,
    salary_min = EXCLUDED.salary_min,
    salary_max = EXCLUDED.salary_max,
    job_type = EXCLUDED.job_type,
    experience_required = EXCLUDED.experience_required,
    status = EXCLUDED.status,
    created_by = EXCLUDED.created_by,
    created_at = EXCLUDED.created_at;

INSERT INTO job_skills (id, job_id, skill_id, skill_name)
VALUES
  (10001, 9901, 9502, 'Spring Boot'),
  (10002, 9902, 9503, 'React'),
  (10003, 9903, 9504, 'PostgreSQL'),
  (10004, 9904, 9505, 'Kafka'),
  (10005, 9905, 9501, 'Java')
ON CONFLICT (id) DO UPDATE
SET job_id = EXCLUDED.job_id,
    skill_id = EXCLUDED.skill_id,
    skill_name = EXCLUDED.skill_name;

-- =========================
-- APPLICATION SERVICE TABLE
-- =========================
INSERT INTO applications (id, job_id, user_id, status, applied_at, updated_at)
VALUES
  (10101, 9901, 9104, 'SHORTLISTED', NOW() - INTERVAL '14 days', NOW() - INTERVAL '8 days'),
  (10102, 9902, 9105, 'APPLIED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
  (10103, 9903, 9104, 'APPLIED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
  (10104, 9904, 9105, 'REJECTED', NOW() - INTERVAL '9 days', NOW() - INTERVAL '3 days'),
  (10105, 9905, 9104, 'HIRED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO UPDATE
SET job_id = EXCLUDED.job_id,
    user_id = EXCLUDED.user_id,
    status = EXCLUDED.status,
    applied_at = EXCLUDED.applied_at,
    updated_at = EXCLUDED.updated_at;

-- =========================
-- NOTIFICATION SERVICE TABLE
-- =========================
INSERT INTO notifications (id, user_id, type, message, status, created_at)
VALUES
  (10201, 9104, 'APPLICATION_SHORTLISTED', 'Your application for Senior Backend Engineer was shortlisted by AstraFin Systems.', 'READ', NOW() - INTERVAL '8 days'),
  (10202, 9105, 'APPLICATION_RECEIVED', 'Your application for Frontend Engineer (React) was received by Nimbus Retail Cloud.', 'UNREAD', NOW() - INTERVAL '12 days'),
  (10203, 9102, 'NEW_APPLICATION', 'A new candidate applied for Full Stack Engineer at QuantaData AI.', 'UNREAD', NOW() - INTERVAL '7 days'),
  (10204, 9103, 'APPLICATION_REJECTED', 'An applicant for Platform SRE has been marked as rejected.', 'ARCHIVED', NOW() - INTERVAL '3 days'),
  (10205, 9101, 'HIRING_ACTIVITY_SUMMARY', 'Weekly hiring summary: 5 open jobs, 5 tracked applications, 1 hire.', 'UNREAD', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    type = EXCLUDED.type,
    message = EXCLUDED.message,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at;

-- Keep identity sequences ahead of explicit IDs
SELECT setval('users_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM users), true);
SELECT setval('refresh_tokens_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM refresh_tokens), true);
SELECT setval('password_reset_tokens_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM password_reset_tokens), true);
SELECT setval('profiles_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM profiles), true);
SELECT setval('skills_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM skills), true);
SELECT setval('user_skills_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM user_skills), true);
SELECT setval('companies_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM companies), true);
SELECT setval('recruiters_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM recruiters), true);
SELECT setval('jobs_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM jobs), true);
SELECT setval('job_skills_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM job_skills), true);
SELECT setval('applications_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM applications), true);
SELECT setval('notifications_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 0), 1) FROM notifications), true);

COMMIT;
