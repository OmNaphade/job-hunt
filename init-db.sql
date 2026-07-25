-- Initialize separate databases for each microservice
-- Run this once when setting up the PostgreSQL instance

CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE job_db;
CREATE DATABASE company_db;
CREATE DATABASE application_db;
CREATE DATABASE notification_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE job_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE company_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE application_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
