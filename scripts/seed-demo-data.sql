-- Demo seed data for local testing
-- Safe to run multiple times due ON CONFLICT clauses.

INSERT INTO users (id, email, password_hash, role, first_name, last_name, created_at)
VALUES
  (1001, 'admin@jobportal.local', '$2a$10$demo.hash.placeholder', 'ADMIN', 'Admin', 'User', NOW()),
  (1002, 'recruiter@jobportal.local', '$2a$10$demo.hash.placeholder', 'RECRUITER', 'Ria', 'Recruiter', NOW()),
  (1003, 'candidate@jobportal.local', '$2a$10$demo.hash.placeholder', 'JOB_SEEKER', 'Cody', 'Candidate', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO companies (id, name, description, website, created_by)
VALUES
  (2001, 'Acme Labs', 'Engineering and R&D company', 'https://acme.local', 1002),
  (2002, 'Nimbus Tech', 'Cloud and platform company', 'https://nimbus.local', 1002)
ON CONFLICT (id) DO NOTHING;

INSERT INTO jobs (id, title, description, location, status, company_id, posted_by, min_salary, max_salary, experience_required)
VALUES
  (3001, 'Backend Engineer', 'Spring Boot microservices role', 'Bengaluru', 'OPEN', 2001, 1002, 1000000, 1800000, 3),
  (3002, 'Frontend Engineer', 'React frontend role', 'Remote', 'OPEN', 2002, 1002, 900000, 1600000, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO applications (id, job_id, applicant_id, status, created_at)
VALUES
  (4001, 3001, 1003, 'SUBMITTED', NOW()),
  (4002, 3002, 1003, 'SUBMITTED', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO notifications (id, user_id, title, message, is_read, created_at)
VALUES
  (5001, 1003, 'Application Submitted', 'Your application was submitted successfully.', false, NOW()),
  (5002, 1002, 'New Candidate Applied', 'A candidate applied to Backend Engineer.', false, NOW())
ON CONFLICT (id) DO NOTHING;
