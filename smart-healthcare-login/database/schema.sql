-- Import this file in XAMPP phpMyAdmin (http://localhost/phpmyadmin)
-- Note: The Java app also auto-creates this database, tables, and demo rows
-- on first login if XAMPP MySQL is running on localhost:3306.

CREATE DATABASE IF NOT EXISTS smart_healthcare
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smart_healthcare;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(32) PRIMARY KEY,
  username VARCHAR(120) NOT NULL UNIQUE,
  full_name VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL,
  salt VARCHAR(64) NOT NULL,
  password_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS join_codes (
  code VARCHAR(16) PRIMARY KEY,
  title VARCHAR(160) NOT NULL,
  department VARCHAR(100) NOT NULL,
  doctor_name VARCHAR(120) NOT NULL,
  expires_at TIMESTAMP NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
