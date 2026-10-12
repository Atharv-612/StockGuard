-- =====================================================================
-- StockGuard Production Database Security Configuration: Least-Privilege User
-- =====================================================================
-- Run this script as the MySQL root or administrative DBA user to create
-- a restricted application account that cannot execute DROP, ALTER, GRANT,
-- or administrative commands.

-- 1. Create dedicated application user (restrict to localhost or app container host)
-- Replace 'StrongPassword123!' with your secure, randomly generated production password.
CREATE USER IF NOT EXISTS 'stockguard_app'@'localhost' IDENTIFIED BY 'StrongPassword123!';
CREATE USER IF NOT EXISTS 'stockguard_app'@'%' IDENTIFIED BY 'StrongPassword123!';

-- 2. Revoke all global privileges
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'stockguard_app'@'localhost';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'stockguard_app'@'%';

-- 3. Grant ONLY Data Manipulation Language (DML) privileges on the stockguard_db database
-- This explicitly excludes DDL (DROP, CREATE, ALTER) and DCL (GRANT, SUPER, PROCESS)
GRANT SELECT, INSERT, UPDATE, DELETE ON stockguard_db.* TO 'stockguard_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON stockguard_db.* TO 'stockguard_app'@'%';

-- 4. Apply changes immediately
FLUSH PRIVILEGES;

-- 5. Verification query to confirm least-privilege boundaries:
SHOW GRANTS FOR 'stockguard_app'@'localhost';
