-- AthleteLink AI - non-destructive MySQL schema/migration
CREATE DATABASE IF NOT EXISTS athletelink_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE athletelink_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    sport VARCHAR(100) NULL,
    location VARCHAR(150) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    achievement_type VARCHAR(40) NULL,
    sport VARCHAR(100) NULL,
    level VARCHAR(40) NULL,
    achievement_year INT NULL,
    academic_year VARCHAR(20) NULL,
    institution VARCHAR(180) NULL,
    position VARCHAR(60) NULL,
    medal_award VARCHAR(120) NULL,
    event_location VARCHAR(150) NULL,
    achieved_on DATE NULL,
    result_label VARCHAR(100) NULL,
    description TEXT NULL,
    certificate_details VARCHAR(255) NULL,
    certificate_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_achievements_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    opportunity_id VARCHAR(80) NOT NULL,
    opportunity_name VARCHAR(180) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_application (user_id, opportunity_id),
    CONSTRAINT fk_application_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(180) NOT NULL UNIQUE,
    sport VARCHAR(100) NOT NULL,
    organization VARCHAR(180) NOT NULL,
    venue VARCHAR(220) NOT NULL,
    trial_date DATE NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'Open',
    age_category VARCHAR(20) NOT NULL DEFAULT 'Youth',
    registration_status VARCHAR(30) NOT NULL DEFAULT 'REG OPEN',
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trial_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trial_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    capacity INT NOT NULL,
    booked_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uq_trial_slot(trial_id, slot_date, slot_time),
    CONSTRAINT fk_trial_slots_trial FOREIGN KEY(trial_id) REFERENCES trials(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trial_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trial_id BIGINT NOT NULL,
    athlete_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    phone VARCHAR(40),
    location VARCHAR(180),
    sport VARCHAR(100) NOT NULL,
    position_role VARCHAR(100) NOT NULL,
    age INT,
    institution VARCHAR(180),
    experience TEXT,
    achievements TEXT,
    introduction TEXT,
    skills TEXT,
    additional_info TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_trial_application(user_id, trial_id),
    CONSTRAINT fk_trial_app_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_trial_app_trial FOREIGN KEY(trial_id) REFERENCES trials(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scouts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    organization VARCHAR(180) NOT NULL,
    sports VARCHAR(180) NOT NULL,
    specialization VARCHAR(180) NOT NULL,
    experience VARCHAR(100) NOT NULL,
    location VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scout_connections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scout_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_scout_connection(user_id, scout_id),
    CONSTRAINT fk_connection_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_connection_scout FOREIGN KEY(scout_id) REFERENCES scouts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trial_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    booking_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    confirmation_token VARCHAR(120) NOT NULL UNIQUE,
    checked_in_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_booking_user_slot(user_id, slot_id),
    CONSTRAINT fk_booking_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_trial FOREIGN KEY(trial_id) REFERENCES trials(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_slot FOREIGN KEY(slot_id) REFERENCES trial_slots(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS athlete_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    category VARCHAR(50) NULL,
    recorded_date DATE NOT NULL,
    notes VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metrics_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS email_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(120) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booking_confirmations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    email VARCHAR(180) NOT NULL,
    confirmation_token VARCHAR(120) NOT NULL UNIQUE,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at DATETIME NULL,
    CONSTRAINT fk_confirmation_booking FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_confirmation_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO trials(name,sport,organization,venue,trial_date,category,age_category,registration_status,featured) VALUES
('U19 Men''s Hockey','Hockey','Hockey India Youth Selection','Major Dhyan Chand National Stadium, New Delhi','2026-09-06','Men''s','U19','REG OPEN',TRUE),
('U21 Women''s Cricket','Cricket','Indian Youth Cricket Selection','M. A. Chidambaram Stadium, Chennai','2026-09-12','Women''s','U21','REG OPEN',TRUE),
('U18 Athletics','Athletics','National Junior Athletics Network','Jawaharlal Nehru Stadium, New Delhi','2026-09-18','Open / Youth','U18','REG OPEN',TRUE),
('U19 Women''s Hockey','Hockey','Hockey India Youth Selection','Kalinga Stadium, Bhubaneswar','2026-09-24','Women''s','U19','REG OPEN',TRUE),
('U21 Men''s Cricket','Cricket','Indian Youth Cricket Selection','M. Chinnaswamy Stadium, Bengaluru','2026-09-29','Men''s','U21','REG OPEN',TRUE);

INSERT IGNORE INTO trials(id,name,sport,organization,venue,trial_date) VALUES
(1,'National Federation Open Trials','Athletics & Track','National Sports Federation','Jawaharlal Nehru Stadium, New Delhi','2026-09-15'),
(2,'Khelo India Regional Trials','Multi-sport','Khelo India','Sree Kanteerava Stadium, Bengaluru','2026-09-22'),
(3,'Youth Boxing Selection Camp','Boxing','National Boxing Federation','Army Sports Institute, Pune','2026-09-30');

INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES
(1,'2026-09-15','09:00:00',10,0),(1,'2026-09-15','11:00:00',12,0),(1,'2026-09-15','14:00:00',8,0),
(2,'2026-09-22','09:00:00',10,0),(2,'2026-09-22','11:00:00',12,0),(2,'2026-09-22','14:00:00',8,0),
(3,'2026-09-30','09:00:00',10,0),(3,'2026-09-30','11:00:00',12,0),(3,'2026-09-30','14:00:00',8,0);

INSERT IGNORE INTO scouts(id,name,organization,sports,specialization,experience,location) VALUES
(1,'Arjun Mehta','National Talent Discovery Network','Athletics, Track & Field','Sprint & performance scouting','12 years','New Delhi'),
(2,'Priya Nair','Elite Youth Sports Collective','Badminton, Athletics','Grassroots talent identification','9 years','Bengaluru'),
(3,'Rahul Singh','National Wrestling Talent Hub','Wrestling, Boxing','Youth & combat sports scouting','15 years','Rohtak');

-- Athlete Benchmark Drills & Skill Challenges (Feature 3)
CREATE TABLE IF NOT EXISTS athlete_challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    drill_type VARCHAR(100) NOT NULL,
    drill_name VARCHAR(150) NOT NULL,
    score DOUBLE NOT NULL,
    unit VARCHAR(40) NOT NULL,
    badge_awarded VARCHAR(100) NULL,
    video_url VARCHAR(255) NULL,
    notes TEXT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_challenge_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Athlete Daily Recovery & Injury Readiness Log (Feature 4)
CREATE TABLE IF NOT EXISTS athlete_readiness_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    readiness_score INT NOT NULL,
    resting_hr INT NULL,
    sleep_hours DOUBLE NULL,
    soreness_level INT NULL,
    fatigue_level INT NULL,
    hydration_liters DOUBLE NULL,
    status_label VARCHAR(60) NOT NULL,
    injury_risk VARCHAR(60) NOT NULL,
    notes TEXT NULL,
    logged_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_readiness_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

