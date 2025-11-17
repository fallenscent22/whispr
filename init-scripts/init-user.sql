-- init-scripts/init-user.sql
-- For local development; set postgres password for SCRAM-SHA-256 authentication
ALTER USER postgres WITH PASSWORD 'postgres';
