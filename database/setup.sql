-- Run once with a PostgreSQL administrator account: psql -U postgres -f database/setup.sql
-- These credentials are for a local development database only.
\set ON_ERROR_STOP on
CREATE USER employee_app WITH PASSWORD 'employee_local_dev';
CREATE DATABASE employees OWNER employee_app;
CREATE DATABASE employees_test OWNER employee_app;
