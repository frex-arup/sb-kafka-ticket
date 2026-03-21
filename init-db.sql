-- Database initialization script for PostgreSQL
-- Creates three databases for different services

-- Create databases
CREATE DATABASE ticketdb;
CREATE DATABASE paymentdb;
CREATE DATABASE userdb;

-- Grant all privileges to postgres user (for learning purposes)
GRANT ALL PRIVILEGES ON DATABASE ticketdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE userdb TO postgres;

-- Note: Tables will be auto-created by Spring Boot JPA (ddl-auto=update)
