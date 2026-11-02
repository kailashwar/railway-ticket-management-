-- =====================================================================
-- RailBook – Railway Ticket Management System
-- Database Schema for MySQL 8.0+
-- College Project / Production Demonstration Database
-- =====================================================================

CREATE DATABASE IF NOT EXISTS railbook_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE railbook_db;

-- 1. STATIONS TABLE
DROP TABLE IF EXISTS verification_audit_log;
DROP TABLE IF EXISTS qr_verifications;
DROP TABLE IF EXISTS identity_documents;
DROP TABLE IF EXISTS passengers;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS trains;
DROP TABLE IF EXISTS stations;
DROP TABLE IF EXISTS ttr_users;
DROP TABLE IF EXISTS users;

CREATE TABLE stations (
    station_id INT AUTO_INCREMENT PRIMARY KEY,
    station_code VARCHAR(10) NOT NULL UNIQUE,
    station_name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    INDEX idx_station_code (station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. TRAINS TABLE
CREATE TABLE trains (
    train_id INT AUTO_INCREMENT PRIMARY KEY,
    train_number VARCHAR(10) NOT NULL UNIQUE,
    train_name VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_time VARCHAR(20) NOT NULL,
    arrival_time VARCHAR(20) NOT NULL,
    duration VARCHAR(30) DEFAULT '7h 30m',
    total_seats INT NOT NULL DEFAULT 120,
    available_seats INT NOT NULL DEFAULT 85,
    ac_seats INT NOT NULL DEFAULT 40,
    sleeper_seats INT NOT NULL DEFAULT 60,
    tatkal_seats INT NOT NULL DEFAULT 20,
    fare_ac DECIMAL(10,2) NOT NULL DEFAULT 1250.00,
    fare_sleeper DECIMAL(10,2) NOT NULL DEFAULT 450.00,
    fare_general DECIMAL(10,2) NOT NULL DEFAULT 180.00,
    tatkal_surcharge DECIMAL(10,2) NOT NULL DEFAULT 150.00,
    runs_on VARCHAR(50) DEFAULT 'Daily',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_train_route (source, destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. USERS TABLE (Passengers & Admins)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('PASSENGER', 'ADMIN') DEFAULT 'PASSENGER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. TTR USERS TABLE (Ticket Checking / Verification Officers)
CREATE TABLE ttr_users (
    ttr_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    badge_number VARCHAR(50) NOT NULL,
    division VARCHAR(100) DEFAULT 'Southern Railway (SR)',
    password_hash VARCHAR(255) NOT NULL,
    status ENUM('ACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. BOOKINGS TABLE
CREATE TABLE bookings (
    booking_id VARCHAR(50) PRIMARY KEY,
    pnr VARCHAR(20) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    train_id INT NOT NULL,
    journey_date DATE NOT NULL,
    source VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    travel_class VARCHAR(50) NOT NULL,
    quota ENUM('General', 'Tatkal') NOT NULL DEFAULT 'General',
    fare DECIMAL(10,2) NOT NULL,
    status ENUM('CONFIRMED', 'WAITING LIST', 'NOT SELECTED', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_train FOREIGN KEY (train_id) REFERENCES trains(train_id) ON DELETE RESTRICT,
    INDEX idx_booking_pnr (pnr),
    INDEX idx_booking_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. PASSENGERS TABLE (Includes Senior Citizen check & Berth Allocation)
CREATE TABLE passengers (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender ENUM('Male', 'Female', 'Transgender') NOT NULL,
    berth_preference VARCHAR(30) NOT NULL,
    nationality VARCHAR(50) DEFAULT 'Indian',
    mobile_number VARCHAR(20),
    coach VARCHAR(20) NOT NULL,
    seat VARCHAR(30) NOT NULL,
    status ENUM('CONFIRMED', 'WAITING LIST', 'NOT SELECTED', 'CANCELLED') DEFAULT 'CONFIRMED',
    is_senior_citizen BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_passenger_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    INDEX idx_passenger_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. IDENTITY DOCUMENTS (Secure Dummy Aadhaar/ID storage)
-- Note: Raw Aadhaar is never exposed or placed in QR code!
CREATE TABLE identity_documents (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    passenger_id INT NOT NULL,
    document_type VARCHAR(50) DEFAULT 'Aadhaar Card (Demo)',
    file_name VARCHAR(255) NOT NULL,
    encrypted_file_reference VARCHAR(255) NOT NULL,
    masked_identifier VARCHAR(50) NOT NULL DEFAULT 'XXXX XXXX 1234',
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') DEFAULT 'VERIFIED',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(passenger_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. QR VERIFICATIONS (Secure Token based verification)
CREATE TABLE qr_verifications (
    qr_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL,
    secure_token_hash VARCHAR(255) NOT NULL UNIQUE,
    raw_token VARCHAR(100) NOT NULL,
    verification_url VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status ENUM('ACTIVE', 'REVOKED', 'EXPIRED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qr_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    INDEX idx_token_hash (secure_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. VERIFICATION AUDIT LOG (TTR access monitoring)
CREATE TABLE verification_audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    ttr_id VARCHAR(50) NOT NULL,
    booking_id VARCHAR(50) NOT NULL,
    verification_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result ENUM('VERIFIED', 'FAILED', 'INVALID_TOKEN', 'CANCELLED') NOT NULL,
    action ENUM('QR_SCAN', 'MANUAL_INPUT', 'VIEW_IDENTITY_DOC') NOT NULL,
    ip_address VARCHAR(50) DEFAULT '127.0.0.1',
    user_agent VARCHAR(255) DEFAULT 'RailBook TTR Terminal',
    notes TEXT,
    CONSTRAINT fk_audit_ttr FOREIGN KEY (ttr_id) REFERENCES ttr_users(ttr_id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- SEED DATA
-- =====================================================================

-- Stations
INSERT INTO stations (station_code, station_name, city, state) VALUES
('MAS', 'Chennai Central', 'Chennai', 'Tamil Nadu'),
('KIK', 'Karaikal', 'Karaikal', 'Puducherry'),
('SBC', 'KSR Bengaluru', 'Bengaluru', 'Karnataka'),
('NDLS', 'New Delhi', 'New Delhi', 'Delhi'),
('MMCT', 'Mumbai Central', 'Mumbai', 'Maharashtra'),
('CBE', 'Coimbatore Junction', 'Coimbatore', 'Tamil Nadu'),
('MDU', 'Madurai Junction', 'Madurai', 'Tamil Nadu'),
('TPJ', 'Tiruchchirappalli Junction', 'Tiruchchirappalli', 'Tamil Nadu'),
('HWH', 'Howrah Junction', 'Kolkata', 'West Bengal');

-- Trains
INSERT INTO trains (train_number, train_name, source, destination, departure_time, arrival_time, duration, total_seats, available_seats, ac_seats, sleeper_seats, tatkal_seats, fare_ac, fare_sleeper, fare_general, tatkal_surcharge, runs_on) VALUES
('12601', 'Sample Express', 'Chennai Central', 'Karaikal', '06:15 AM', '01:45 PM', '7h 30m', 120, 85, 40, 60, 20, 1250.00, 450.00, 180.00, 150.00, 'Daily'),
('12675', 'Kovai Superfast Express', 'Chennai Central', 'Coimbatore Junction', '06:10 AM', '02:05 PM', '7h 55m', 150, 92, 50, 80, 20, 1320.00, 480.00, 195.00, 175.00, 'Daily'),
('12007', 'Shatabdi Express', 'Chennai Central', 'KSR Bengaluru', '06:00 AM', '11:00 AM', '5h 00m', 100, 48, 80, 0, 20, 1450.00, 750.00, 250.00, 200.00, 'Except Tuesday'),
('12637', 'Pandian Superfast Express', 'Chennai Central', 'Madurai Junction', '09:40 PM', '05:35 AM', '7h 55m', 140, 64, 45, 75, 20, 1280.00, 460.00, 190.00, 160.00, 'Daily'),
('12243', 'Shatabdi Duronto Express', 'Chennai Central', 'New Delhi', '06:35 AM', '10:30 AM', '27h 55m', 180, 110, 120, 40, 20, 3450.00, 1200.00, 550.00, 350.00, 'Mon, Wed, Fri');

-- Users:
-- Admin: kailashwar24@gmail.com / Kailash@24 (BCrypt: $2a$10$eE5/3.r0K4k5r24U9W0nheVjJ4d9YvV7xQk9C6fK/tJq1n8b0s8s2 or standard hash)
-- Sample Passenger: demo@railbook.com / Pass@123
INSERT INTO users (name, email, phone, password_hash, role) VALUES
('System Administrator', 'kailashwar24@gmail.com', '9876543210', '$2a$10$w850Xf3.jBwQ7h7701LzC.OqOwhUv0qG6/O1n031G36vN3gW9qE4q', 'ADMIN'),
('Rajesh Sharma', 'demo@railbook.com', '9840123456', '$2a$10$w850Xf3.jBwQ7h7701LzC.OqOwhUv0qG6/O1n031G36vN3gW9qE4q', 'PASSENGER'),
('Priya Sundaram', 'priya.s@example.com', '9444123890', '$2a$10$w850Xf3.jBwQ7h7701LzC.OqOwhUv0qG6/O1n031G36vN3gW9qE4q', 'PASSENGER');

-- TTR User: TTR-2026 / ttr_officer / TTR@123
INSERT INTO ttr_users (ttr_id, name, username, badge_number, division, password_hash, status) VALUES
('TTR-2026', 'Inspector K. Ramanathan', 'ttr_officer', 'SR-TTR-8942', 'Southern Railway - Chennai Division', '$2a$10$w850Xf3.jBwQ7h7701LzC.OqOwhUv0qG6/O1n031G36vN3gW9qE4q', 'ACTIVE');

-- Sample Bookings:
-- Booking 1: Sample Express Chennai -> Karaikal (Senior Citizen with Lower Berth auto allocated!)
INSERT INTO bookings (booking_id, pnr, user_id, train_id, journey_date, source, destination, travel_class, quota, fare, status) VALUES
('RB20261029001', '4567891234', 2, 1, '2026-10-30', 'Chennai Central', 'Karaikal', 'Sleeper', 'Tatkal', 600.00, 'CONFIRMED'),
('RB20261029002', '7823456109', 2, 2, '2026-11-05', 'Chennai Central', 'Coimbatore Junction', 'AC 3 Tier', 'General', 1320.00, 'WAITING LIST'),
('RB20261029003', '9102384756', 3, 3, '2026-11-12', 'Chennai Central', 'KSR Bengaluru', 'Executive AC', 'General', 1450.00, 'CONFIRMED');

-- Passengers for Booking 1: Senior Citizen age 60 (Lower Berth allocated)
INSERT INTO passengers (booking_id, name, age, gender, berth_preference, nationality, mobile_number, coach, seat, status, is_senior_citizen) VALUES
('RB20261029001', 'Venkatesan Raman', 65, 'Male', 'Lower', 'Indian', '9840123456', 'S3', '18 (Lower Berth)', 'CONFIRMED', TRUE),
('RB20261029002', 'Priya Sundaram', 28, 'Female', 'Upper', 'Indian', '9444123890', 'B2', 'WL/4', 'WAITING LIST', FALSE),
('RB20261029003', 'Anand Krishnan', 34, 'Male', 'Side Lower', 'Indian', '9789012345', 'C1', '22 (Side Lower)', 'CONFIRMED', FALSE);

-- Identity documents for sample passengers (Masked, dummy test documents only)
INSERT INTO identity_documents (passenger_id, document_type, file_name, encrypted_file_reference, masked_identifier, verification_status) VALUES
(1, 'Aadhaar Card (Demo)', 'dummy_aadhaar_senior_65.pdf', 'uploads/dummy_aadhaar_senior_65.pdf', 'XXXX XXXX 8921', 'VERIFIED'),
(2, 'Voter ID (Demo)', 'dummy_voter_id_sample.jpg', 'uploads/dummy_voter_id_sample.jpg', 'XXXX XXXX 3410', 'VERIFIED'),
(3, 'Aadhaar Card (Demo)', 'dummy_aadhaar_anand.pdf', 'uploads/dummy_aadhaar_anand.pdf', 'XXXX XXXX 6754', 'VERIFIED');

-- QR Verification records with Secure Tokens (Raw tokens used for testing QR scan)
-- The QR code image contains ONLY the raw token string (or verification URL)!
INSERT INTO qr_verifications (booking_id, secure_token_hash, raw_token, verification_url, expires_at, status) VALUES
('RB20261029001', SHA2('RB-SECURE-TOKEN-4567891234-9912a', 256), 'RB-SECURE-TOKEN-4567891234-9912a', 'https://railbook.internal/ttr/verify/RB-SECURE-TOKEN-4567891234-9912a', '2026-11-01 23:59:59', 'ACTIVE'),
('RB20261029002', SHA2('RB-SECURE-TOKEN-7823456109-77a1b', 256), 'RB-SECURE-TOKEN-7823456109-77a1b', 'https://railbook.internal/ttr/verify/RB-SECURE-TOKEN-7823456109-77a1b', '2026-11-06 23:59:59', 'ACTIVE'),
('RB20261029003', SHA2('RB-SECURE-TOKEN-9102384756-33c8f', 256), 'RB-SECURE-TOKEN-9102384756-33c8f', 'https://railbook.internal/ttr/verify/RB-SECURE-TOKEN-9102384756-33c8f', '2026-11-13 23:59:59', 'ACTIVE');

-- Sample Initial Verification Audit Log
INSERT INTO verification_audit_log (ttr_id, booking_id, result, action, notes) VALUES
('TTR-2026', 'RB20261029001', 'VERIFIED', 'QR_SCAN', 'Initial boarding verification successful at MAS station');
