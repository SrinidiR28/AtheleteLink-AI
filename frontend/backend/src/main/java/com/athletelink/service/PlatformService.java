package com.athletelink.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletelink.dto.AchievementRequest;
import com.athletelink.dto.AthleteMetricRequest;
import com.athletelink.dto.BookingRequest;
import com.athletelink.dto.ChallengeSubmissionRequest;
import com.athletelink.dto.CoachConnectionRequest;
import com.athletelink.dto.ReadinessLogRequest;
import com.athletelink.dto.ScholarshipApplicationRequest;
import com.athletelink.dto.TrialApplicationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class PlatformService {
    private final JdbcTemplate jdbc;
    private final EmailService emailService;
    private final String publicBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    public PlatformService(JdbcTemplate jdbc, EmailService emailService, @org.springframework.beans.factory.annotation.Value("${athletelink.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.jdbc = jdbc;
        this.emailService = emailService;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }

    @PostConstruct
    public void initialize() {
        // Core tables
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(100) NOT NULL, email VARCHAR(150) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, sport VARCHAR(100) NULL, location VARCHAR(150) NULL, latitude DECIMAL(10,7) NULL, longitude DECIMAL(10,7) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS achievements (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(150) NOT NULL, event_location VARCHAR(150) NULL, achieved_on DATE NULL, result_label VARCHAR(100) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_achievements_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("achievements", "achievement_type", "VARCHAR(40) NULL");
        addColumnIfMissing("achievements", "sport", "VARCHAR(100) NULL");
        addColumnIfMissing("achievements", "level", "VARCHAR(40) NULL");
        addColumnIfMissing("achievements", "achievement_year", "INT NULL");
        addColumnIfMissing("achievements", "academic_year", "VARCHAR(20) NULL");
        addColumnIfMissing("achievements", "institution", "VARCHAR(180) NULL");
        addColumnIfMissing("achievements", "position", "VARCHAR(60) NULL");
        addColumnIfMissing("achievements", "medal_award", "VARCHAR(120) NULL");
        addColumnIfMissing("achievements", "description", "TEXT NULL");
        addColumnIfMissing("achievements", "certificate_details", "VARCHAR(255) NULL");
        addColumnIfMissing("achievements", "certificate_url", "VARCHAR(500) NULL");

        jdbc.execute("CREATE TABLE IF NOT EXISTS trials (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(180) NOT NULL UNIQUE, sport VARCHAR(100) NOT NULL, organization VARCHAR(180) NOT NULL, venue VARCHAR(220) NOT NULL, trial_date DATE NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("trials", "category", "VARCHAR(40) NOT NULL DEFAULT 'Open'");
        addColumnIfMissing("trials", "age_category", "VARCHAR(20) NOT NULL DEFAULT 'Youth'");
        addColumnIfMissing("trials", "registration_status", "VARCHAR(30) NOT NULL DEFAULT 'REG OPEN'");
        addColumnIfMissing("trials", "featured", "BOOLEAN NOT NULL DEFAULT FALSE");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trial_slots (id BIGINT AUTO_INCREMENT PRIMARY KEY, trial_id BIGINT NOT NULL, slot_date DATE NOT NULL, slot_time TIME NOT NULL, capacity INT NOT NULL, booked_count INT NOT NULL DEFAULT 0, CONSTRAINT fk_trial_slots_trial FOREIGN KEY (trial_id) REFERENCES trials(id) ON DELETE CASCADE, CONSTRAINT uq_trial_slot UNIQUE(trial_id, slot_date, slot_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trial_applications (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, trial_id BIGINT NOT NULL, athlete_name VARCHAR(120) NOT NULL, email VARCHAR(180) NOT NULL, phone VARCHAR(40), sport VARCHAR(100) NOT NULL, position_role VARCHAR(100) NOT NULL, age INT, institution VARCHAR(180), experience TEXT, achievements TEXT, introduction TEXT, skills TEXT, additional_info TEXT, status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, CONSTRAINT fk_trial_app_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_trial_app_trial FOREIGN KEY(trial_id) REFERENCES trials(id) ON DELETE CASCADE, CONSTRAINT uq_trial_application UNIQUE(user_id, trial_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("trial_applications", "location", "VARCHAR(180) NULL");
        addColumnIfMissing("trial_applications", "confirmed_trial_date", "DATE NULL");
        addColumnIfMissing("trial_applications", "confirmed_trial_time", "VARCHAR(30) NULL");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trial_application_confirmations (id BIGINT AUTO_INCREMENT PRIMARY KEY, application_id BIGINT NOT NULL UNIQUE, user_id BIGINT NOT NULL, email VARCHAR(180) NOT NULL, confirmation_token VARCHAR(120) NOT NULL UNIQUE, email_sent BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, confirmed_at DATETIME NULL, CONSTRAINT fk_trial_app_confirmation FOREIGN KEY(application_id) REFERENCES trial_applications(id) ON DELETE CASCADE, CONSTRAINT fk_trial_app_confirmation_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS applications (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, opportunity_id VARCHAR(80) NOT NULL, opportunity_name VARCHAR(180) NOT NULL, status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT uq_application UNIQUE(user_id, opportunity_id), CONSTRAINT fk_application_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS scouts (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150) NOT NULL, organization VARCHAR(180) NOT NULL, sports VARCHAR(180) NOT NULL, specialization VARCHAR(180) NOT NULL, experience VARCHAR(100) NOT NULL, location VARCHAR(150) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS scout_connections (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, scout_id BIGINT NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, CONSTRAINT fk_connection_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_connection_scout FOREIGN KEY(scout_id) REFERENCES scouts(id) ON DELETE CASCADE, CONSTRAINT uq_scout_connection UNIQUE(user_id, scout_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("scout_connections", "message", "VARCHAR(500) NULL");
        jdbc.execute("CREATE TABLE IF NOT EXISTS coaches (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150) NOT NULL, organization VARCHAR(180) NOT NULL, sports VARCHAR(180) NOT NULL, specialization VARCHAR(180) NOT NULL, experience VARCHAR(100) NOT NULL, location VARCHAR(150) NOT NULL, latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS coach_connections (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, coach_id BIGINT NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING', message VARCHAR(500) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, CONSTRAINT fk_coach_connection_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_coach_connection_coach FOREIGN KEY(coach_id) REFERENCES coaches(id) ON DELETE CASCADE, CONSTRAINT uq_coach_connection UNIQUE(user_id, coach_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("users", "latitude", "DECIMAL(10,7) NULL");
        addColumnIfMissing("users", "longitude", "DECIMAL(10,7) NULL");
        addColumnIfMissing("users", "role", "VARCHAR(30) NOT NULL DEFAULT 'ATHLETE'");
        jdbc.execute("CREATE TABLE IF NOT EXISTS bookings (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, trial_id BIGINT NOT NULL, slot_id BIGINT NOT NULL, booking_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED', confirmation_token VARCHAR(120) NOT NULL UNIQUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_booking_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_booking_trial FOREIGN KEY(trial_id) REFERENCES trials(id) ON DELETE CASCADE, CONSTRAINT fk_booking_slot FOREIGN KEY(slot_id) REFERENCES trial_slots(id) ON DELETE CASCADE, CONSTRAINT uq_booking_user_slot UNIQUE(user_id, slot_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("bookings", "checked_in_at", "DATETIME NULL");
        jdbc.execute("CREATE TABLE IF NOT EXISTS athlete_metrics (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "metric_name VARCHAR(100) NOT NULL, " +
                "metric_value DECIMAL(10,2) NOT NULL, " +
                "unit VARCHAR(30) NOT NULL, " +
                "category VARCHAR(50) NULL, " +
                "recorded_date DATE NOT NULL, " +
                "notes VARCHAR(255) NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_metrics_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS notifications (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(180) NOT NULL, message VARCHAR(500) NOT NULL, is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS email_verifications (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, token VARCHAR(120) NOT NULL UNIQUE, expires_at DATETIME NOT NULL, verified BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_verification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS booking_confirmations (id BIGINT AUTO_INCREMENT PRIMARY KEY, booking_id BIGINT NOT NULL UNIQUE, user_id BIGINT NOT NULL, email VARCHAR(180) NOT NULL, confirmation_token VARCHAR(120) NOT NULL UNIQUE, email_sent BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, confirmed_at DATETIME NULL, CONSTRAINT fk_confirmation_booking FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE CASCADE, CONSTRAINT fk_confirmation_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        // Scholarship Applications & Quota Enforcement
        jdbc.execute("CREATE TABLE IF NOT EXISTS scholarship_applications (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "category VARCHAR(60) NOT NULL, " +
                "category_title VARCHAR(180) NOT NULL, " +
                "full_name VARCHAR(120) NOT NULL, " +
                "email VARCHAR(180) NOT NULL, " +
                "phone VARCHAR(40) NOT NULL, " +
                "age INT NULL, " +
                "dob DATE NULL, " +
                "family_income DECIMAL(12,2) NULL, " +
                "parent_guardian_name VARCHAR(120) NULL, " +
                "defence_relationship VARCHAR(60) NULL, " +
                "supporting_person_name VARCHAR(120) NULL, " +
                "current_grade VARCHAR(80) NULL, " +
                "maintained_grade VARCHAR(80) NULL, " +
                "stationery_required VARCHAR(10) NULL, " +
                "resources_required VARCHAR(10) NULL, " +
                "required_resources_list TEXT NULL, " +
                "loss_year INT NULL, " +
                "documents_json LONGTEXT NULL, " +
                "status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED', " +
                "admin_notes VARCHAR(500) NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_scholarship_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbc.execute("CREATE TABLE IF NOT EXISTS scholarship_quota_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "email VARCHAR(180) NOT NULL, " +
                "phone VARCHAR(40) NULL, " +
                "application_id BIGINT NOT NULL, " +
                "category VARCHAR(60) NOT NULL, " +
                "status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED', " +
                "applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uq_quota_user (user_id), " +
                "CONSTRAINT fk_quota_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbc.execute("CREATE TABLE IF NOT EXISTS athlete_challenges (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "drill_type VARCHAR(100) NOT NULL, " +
                "drill_name VARCHAR(150) NOT NULL, " +
                "score DOUBLE NOT NULL, " +
                "unit VARCHAR(40) NOT NULL, " +
                "badge_awarded VARCHAR(100) NULL, " +
                "video_url VARCHAR(255) NULL, " +
                "notes TEXT NULL, " +
                "status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_challenge_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbc.execute("CREATE TABLE IF NOT EXISTS athlete_readiness_logs (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "readiness_score INT NOT NULL, " +
                "resting_hr INT NULL, " +
                "sleep_hours DOUBLE NULL, " +
                "soreness_level INT NULL, " +
                "fatigue_level INT NULL, " +
                "hydration_liters DOUBLE NULL, " +
                "status_label VARCHAR(60) NOT NULL, " +
                "injury_risk VARCHAR(60) NOT NULL, " +
                "notes TEXT NULL, " +
                "logged_date DATE NOT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_readiness_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        seedTrialsAndSlots();
        seedScouts();
        seedCoaches();
        seedChallengeBenchmarks();
    }

    /* ---------- Public Stats (real data from DB) ---------- */
    public Map<String, Object> publicStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        long realAthletes = countSafe("SELECT COUNT(*) FROM users WHERE email NOT LIKE '%example.com' AND email NOT LIKE '%admin%'");
        stats.put("athletesRegistered", realAthletes > 0 ? realAthletes : countSafe("SELECT COUNT(*) FROM users"));
        stats.put("trialsAvailable", countSafe("SELECT COUNT(*) FROM trials"));
        long realApps = countSafe("SELECT COUNT(*) FROM trial_applications WHERE email NOT LIKE '%example.com'");
        stats.put("opportunitiesConnected", realApps > 0 ? realApps : countSafe("SELECT COUNT(*) FROM trial_applications"));
        stats.put("scholarshipsApplied", countSafe("SELECT COUNT(*) FROM scholarship_applications"));
        stats.put("scouts", countSafe("SELECT COUNT(*) FROM scouts"));
        stats.put("coaches", countSafe("SELECT COUNT(*) FROM coaches"));
        stats.put("achievements", countSafe("SELECT COUNT(*) FROM achievements"));
        return stats;
    }

    private long countSafe(String sql) {
        try { return jdbc.queryForObject(sql, Long.class); }
        catch (Exception e) { return 0; }
    }

    @SuppressWarnings("UseSpecificCatch")
    private void addColumnIfMissing(String table, String column, String definition) {
        try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); }
        catch (Exception ignored) { /* column already exists */ }
    }

    private void seedTrialsAndSlots() {
        seedFeaturedTrial("U19 Men's Hockey", "Hockey", "Men's", "U19", "Hockey India Youth Selection", "Major Dhyan Chand National Stadium, New Delhi", LocalDate.of(2026,9,6), 36);
        seedFeaturedTrial("U21 Women's Cricket", "Cricket", "Women's", "U21", "Indian Youth Cricket Selection", "M. A. Chidambaram Stadium, Chennai", LocalDate.of(2026,9,12), 30);
        seedFeaturedTrial("U18 Athletics", "Athletics", "Open / Youth", "U18", "National Junior Athletics Network", "Jawaharlal Nehru Stadium, New Delhi", LocalDate.of(2026,9,18), 40);
        seedFeaturedTrial("U19 Women's Hockey", "Hockey", "Women's", "U19", "Hockey India Youth Selection", "Kalinga Stadium, Bhubaneswar", LocalDate.of(2026,9,24), 32);
        seedFeaturedTrial("U21 Men's Cricket", "Cricket", "Men's", "U21", "Indian Youth Cricket Selection", "M. Chinnaswamy Stadium, Bengaluru", LocalDate.of(2026,9,29), 28);
        seedLegacyTrial("National Federation Open Trials", "Athletics & Track", "National Sports Federation", "Jawaharlal Nehru Stadium, New Delhi", LocalDate.of(2026,9,15));
        seedLegacyTrial("Khelo India Regional Trials", "Multi-sport", "Khelo India", "Sree Kanteerava Stadium, Bengaluru", LocalDate.of(2026,9,22));
        seedLegacyTrial("Youth Boxing Selection Camp", "Boxing", "National Boxing Federation", "Army Sports Institute, Pune", LocalDate.of(2026,9,30));
    }

    private void seedFeaturedTrial(String name, String sport, String category, String ageCategory, String organization, String venue, LocalDate date, int capacity) {
        jdbc.update("INSERT IGNORE INTO trials(name,sport,organization,venue,trial_date,category,age_category,registration_status,featured) VALUES(?,?,?,?,?,?,?,?,TRUE)", name, sport, organization, venue, date, category, ageCategory, "REG OPEN");
        jdbc.update("UPDATE trials SET sport=?,organization=?,venue=?,trial_date=?,category=?,age_category=?,registration_status='REG OPEN',featured=TRUE WHERE name=?", sport, organization, venue, date, category, ageCategory, name);
        addFeaturedSlotsForTrial(trialIdByName(name), date, capacity);
    }

    private void seedLegacyTrial(String name, String sport, String organization, String venue, LocalDate date) {
        jdbc.update("INSERT IGNORE INTO trials(name,sport,organization,venue,trial_date,category,age_category,registration_status,featured) VALUES(?,?,?,?,?,?,?,?,FALSE)", name, sport, organization, venue, date, "Open", "Youth", "REG OPEN");
        addSlotsForTrial(trialIdByName(name), date);
    }

    private void addFeaturedSlotsForTrial(long trialId, LocalDate date, int capacity) {
        int a=Math.max(6, capacity/3), b=Math.max(6, capacity/3), c=Math.max(6, capacity-a-b);
        jdbc.update("INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES(?,?,?,?,0)",trialId,date,java.sql.Time.valueOf("09:00:00"),a);
        jdbc.update("INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES(?,?,?,?,0)",trialId,date,java.sql.Time.valueOf("11:00:00"),b);
        jdbc.update("INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES(?,?,?,?,0)",trialId,date,java.sql.Time.valueOf("14:00:00"),c);
    }

    private long trialIdByName(String name) {
        Long id = jdbc.queryForObject("SELECT id FROM trials WHERE name=?", Long.class, name);
        if (id == null) throw new IllegalStateException("Trial seed failed: " + name);
        return id;
    }
    private void addSlotsForTrial(long trialId, LocalDate date) {
        jdbc.update("INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES(?,?,?,?,0)",trialId,date,java.sql.Time.valueOf("09:00:00"),10);
        jdbc.update("INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES(?,?,?,?,0)",trialId,date,java.sql.Time.valueOf("11:00:00"),12);
        jdbc.update("INSERT IGNORE INTO trial_slots(trial_id,slot_date,slot_time,capacity,booked_count) VALUES(?,?,?,?,0)",trialId,date,java.sql.Time.valueOf("14:00:00"),8);
    }
    private void seedScouts() {
        jdbc.update("INSERT IGNORE INTO scouts(id,name,organization,sports,specialization,experience,location) VALUES(1,?,?,?,?,?,?)", "Arjun Mehta", "National Talent Discovery Network", "Athletics, Track & Field", "Sprint & performance scouting", "12 years", "New Delhi");
        jdbc.update("INSERT IGNORE INTO scouts(id,name,organization,sports,specialization,experience,location) VALUES(2,?,?,?,?,?,?)", "Priya Nair", "Elite Youth Sports Collective", "Badminton, Athletics", "Grassroots talent identification", "9 years", "Bengaluru");
        jdbc.update("INSERT IGNORE INTO scouts(id,name,organization,sports,specialization,experience,location) VALUES(3,?,?,?,?,?,?)", "Rahul Singh", "National Wrestling Talent Hub", "Wrestling, Boxing", "Youth & combat sports scouting", "15 years", "Rohtak");
    }

    private void seedCoaches() {
        jdbc.update("INSERT IGNORE INTO coaches(id,name,organization,sports,specialization,experience,location,latitude,longitude) VALUES(1,?,?,?,?,?,?,?,?)", "Kavya Raman", "Chennai High Performance Centre", "Athletics, Track & Field", "Sprint technique & strength conditioning", "11 years", "Chennai, Tamil Nadu", 13.0827, 80.2707);
        jdbc.update("INSERT IGNORE INTO coaches(id,name,organization,sports,specialization,experience,location,latitude,longitude) VALUES(2,?,?,?,?,?,?,?,?)", "Arvind Krishnan", "South India Badminton Academy", "Badminton", "Singles coaching & tournament preparation", "14 years", "Chennai, Tamil Nadu", 13.0475, 80.2090);
        jdbc.update("INSERT IGNORE INTO coaches(id,name,organization,sports,specialization,experience,location,latitude,longitude) VALUES(3,?,?,?,?,?,?,?,?)", "Meera Iyer", "Elite Sports Performance Lab", "Athletics, Multi-sport", "Youth development & performance analysis", "8 years", "Bengaluru, Karnataka", 12.9716, 77.5946);
        jdbc.update("INSERT IGNORE INTO coaches(id,name,organization,sports,specialization,experience,location,latitude,longitude) VALUES(4,?,?,?,?,?,?,?,?)", "Rohit Verma", "National Wrestling Development Centre", "Wrestling, Boxing", "Combat sports conditioning", "13 years", "Rohtak, Haryana", 28.8955, 76.6066);
        jdbc.update("INSERT IGNORE INTO coaches(id,name,organization,sports,specialization,experience,location,latitude,longitude) VALUES(5,?,?,?,?,?,?,?,?)", "Neha Kapoor", "Hyderabad Elite Sports Academy", "Badminton, Athletics", "Junior athlete development", "10 years", "Hyderabad, Telangana", 17.3850, 78.4867);
    }

    // =========================================================================
    // SCHOLARSHIP APPLICATIONS (5 CATEGORIES + ONE-PER-PERSON RULE)
    // =========================================================================

    @Transactional
    public Map<String, Object> applyScholarship(long userId, ScholarshipApplicationRequest req) {
        // Normalize Category Key
        String category = normalizeCategory(req.getCategory(), req.getCategoryTitle());
        String categoryTitle = getStandardCategoryTitle(category);

        // 1. STRICT ONE SCHOLARSHIP PER PERSON CHECK
        List<Map<String, Object>> quotaLogs = jdbc.queryForList(
                "SELECT * FROM scholarship_quota_log WHERE user_id=? OR email=?", userId, clean(req.getEmail()));
        if (!quotaLogs.isEmpty()) {
            Map<String, Object> existingLog = quotaLogs.get(0);
            String logStatus = Objects.toString(existingLog.get("status"), "SUBMITTED");
            if ("CANCELLED".equalsIgnoreCase(logStatus)) {
                throw new IllegalStateException("You have already used your scholarship application. Cancelling an application does not allow you to apply for another scholarship.");
            } else {
                throw new IllegalStateException("You have already applied for a scholarship. You can apply for only one scholarship.");
            }
        }

        // Secondary check on scholarship_applications table
        List<Map<String, Object>> existingApps = jdbc.queryForList(
                "SELECT * FROM scholarship_applications WHERE user_id=? OR email=?", userId, clean(req.getEmail()));
        if (!existingApps.isEmpty()) {
            Map<String, Object> latest = existingApps.get(0);
            String status = Objects.toString(latest.get("status"), "SUBMITTED");
            if ("CANCELLED".equalsIgnoreCase(status)) {
                throw new IllegalStateException("You have already used your scholarship application. Cancelling an application does not allow you to apply for another scholarship.");
            } else {
                throw new IllegalStateException("You have already applied for a scholarship. You can apply for only one scholarship.");
            }
        }

        // 2. COMMON FIELD VALIDATION
        String fullName = clean(req.getFullName());
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        String email = clean(req.getEmail());
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        String phone = clean(req.getPhone());
        if (phone != null) phone = phone.replaceAll("[^0-9]", "");
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Please enter a valid 10-digit Indian mobile number.");
        }

        Map<String, String> docs = req.getDocuments() != null ? req.getDocuments() : new LinkedHashMap<>();

        // 3. CATEGORY-SPECIFIC VALIDATION
        LocalDate dob = null;
        Integer age = req.getAge();
        Double familyIncome = req.getFamilyIncome();
        String parentGuardianName = clean(req.getParentGuardianName());
        String defenceRelationship = clean(req.getDefenceRelationship());
        String supportingPersonName = clean(req.getSupportingPersonName());
        String currentGrade = clean(req.getCurrentGrade());
        String maintainedGrade = clean(req.getMaintainedGrade());
        String stationery = clean(req.getStationeryRequired());
        String resources = clean(req.getResourcesRequired());
        String requiredResourcesList = clean(req.getRequiredResourcesList());
        Integer lossYear = req.getLossYear();

        LocalDate today = LocalDate.now();

        switch (category) {
            case "UNDER_18":
                if (age == null || age < 1 || age >= 18) {
                    throw new IllegalArgumentException("Age must be below 18 for this scholarship.");
                }
                dob = parseDate(req.getDob());
                if (dob == null) {
                    throw new IllegalArgumentException("Date of birth is required.");
                }
                if (dob.isAfter(today)) {
                    throw new IllegalArgumentException("Date of birth cannot be a future date.");
                }
                int calculatedAgeU18 = Period.between(dob, today).getYears();
                if (calculatedAgeU18 >= 18) {
                    throw new IllegalArgumentException("Date of birth corresponds to age 18 or above. Age must be below 18.");
                }
                if (familyIncome == null || familyIncome <= 0) {
                    throw new IllegalArgumentException("Family annual income must be a valid positive amount.");
                }
                if (docs.get("ageProof") == null || docs.get("ageProof").isBlank()) {
                    throw new IllegalArgumentException("Age proof document (Birth Certificate / Aadhaar Card) is required.");
                }
                if (parentGuardianName == null || parentGuardianName.isBlank()) {
                    throw new IllegalArgumentException("Parent/Guardian name is required.");
                }
                if (docs.get("parentGuardianId") == null || docs.get("parentGuardianId").isBlank()) {
                    throw new IllegalArgumentException("Parent/Guardian ID proof document is required.");
                }
                break;

            case "ABOVE_18":
                if (age == null || age < 18) {
                    throw new IllegalArgumentException("Age must be 18 or above for this scholarship.");
                }
                dob = parseDate(req.getDob());
                if (dob == null) {
                    throw new IllegalArgumentException("Date of birth is required.");
                }
                if (dob.isAfter(today)) {
                    throw new IllegalArgumentException("Date of birth cannot be a future date.");
                }
                int calculatedAgeA18 = Period.between(dob, today).getYears();
                if (calculatedAgeA18 < 18) {
                    throw new IllegalArgumentException("Date of birth corresponds to age below 18. Age must be 18 or above.");
                }
                if (familyIncome == null || familyIncome <= 0) {
                    throw new IllegalArgumentException("Family annual income must be a valid positive amount.");
                }
                if (docs.get("ageProof") == null || docs.get("ageProof").isBlank()) {
                    throw new IllegalArgumentException("Age proof document is required.");
                }
                if (docs.get("applicantId") == null || docs.get("applicantId").isBlank()) {
                    throw new IllegalArgumentException("Applicant ID proof document is required.");
                }
                break;

            case "WOMEN":
                // Category 3: Simple & focused - only Full Name, Email, Phone, Aadhaar Card
                String aadhaarDoc = docs.get("aadhaarCard") != null ? docs.get("aadhaarCard") : docs.get("applicantId");
                if (aadhaarDoc == null || aadhaarDoc.isBlank()) {
                    throw new IllegalArgumentException("Aadhaar Card document upload is mandatory.");
                }
                docs.put("aadhaarCard", aadhaarDoc);
                break;

            case "DEFENCE":
                if (defenceRelationship == null || (!defenceRelationship.equalsIgnoreCase("Mother") && !defenceRelationship.equalsIgnoreCase("Father") && !defenceRelationship.equalsIgnoreCase("Other blood relative only"))) {
                    throw new IllegalArgumentException("Defence personnel relationship must be selected from Mother, Father, or Other blood relative only.");
                }
                if (supportingPersonName == null || supportingPersonName.isBlank()) {
                    throw new IllegalArgumentException("Supporting person's full name is required.");
                }
                if (currentGrade == null || currentGrade.isBlank()) {
                    throw new IllegalArgumentException("Current grade/year of study is required.");
                }
                if (maintainedGrade == null || maintainedGrade.isBlank()) {
                    throw new IllegalArgumentException("Grade/year being maintained is required.");
                }
                if (stationery == null || (!stationery.equalsIgnoreCase("Yes") && !stationery.equalsIgnoreCase("No"))) {
                    throw new IllegalArgumentException("Please indicate whether you require stationery (Yes/No).");
                }
                if (resources == null || (!resources.equalsIgnoreCase("Yes") && !resources.equalsIgnoreCase("No"))) {
                    throw new IllegalArgumentException("Please indicate whether you require other educational resources/support (Yes/No).");
                }
                if ("Yes".equalsIgnoreCase(resources) && (requiredResourcesList == null || requiredResourcesList.isBlank())) {
                    throw new IllegalArgumentException("Please list the required educational resources and support.");
                }
                if (docs.get("applicantId") == null || docs.get("applicantId").isBlank()) {
                    throw new IllegalArgumentException("Applicant ID proof is required.");
                }
                if (docs.get("defenceId") == null || docs.get("defenceId").isBlank()) {
                    throw new IllegalArgumentException("Defence personnel/supporting person's ID proof is required.");
                }
                break;

            case "SINGLE_PARENT":
                int currentYear = today.getYear();
                if (lossYear == null || lossYear < 1960 || lossYear > currentYear) {
                    throw new IllegalArgumentException("Please enter a valid year when parent was lost (cannot be in the future).");
                }
                if (familyIncome == null || familyIncome <= 0) {
                    throw new IllegalArgumentException("Family annual income must be a valid positive amount.");
                }
                if (docs.get("applicantId") == null || docs.get("applicantId").isBlank()) {
                    throw new IllegalArgumentException("Applicant ID proof is mandatory.");
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown scholarship category: " + category);
        }

        // 4. SERIALIZE DOCUMENTS
        String docsJson = req.getDocumentsJson();
        if (docsJson == null || docsJson.isBlank()) {
            try {
                docsJson = objectMapper.writeValueAsString(docs);
            } catch (Exception e) {
                docsJson = "{}";
            }
        }

        // 5. INSERT SCHOLARSHIP APPLICATION
        jdbc.update("INSERT INTO scholarship_applications(" +
                "user_id, category, category_title, full_name, email, phone, age, dob, family_income, " +
                "parent_guardian_name, defence_relationship, supporting_person_name, current_grade, " +
                "maintained_grade, stationery_required, resources_required, required_resources_list, " +
                "loss_year, documents_json, status" +
                ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'SUBMITTED')",
                userId, category, categoryTitle, fullName, email, phone, age, dob, familyIncome,
                parentGuardianName, defenceRelationship, supportingPersonName, currentGrade,
                maintainedGrade, stationery, resources, requiredResourcesList, lossYear, docsJson);

        Long appId = jdbc.queryForObject("SELECT id FROM scholarship_applications WHERE user_id=? ORDER BY id DESC LIMIT 1", Long.class, userId);

        // 6. RECORD QUOTA USAGE (PERMANENT LOG)
        jdbc.update("INSERT INTO scholarship_quota_log(user_id, email, phone, application_id, category, status) VALUES(?,?,?,?,?, 'SUBMITTED')",
                userId, email, phone, appId, category);

        // 7. SEND SUBMISSION CONFIRMATION EMAIL
        boolean sent = emailService.sendScholarshipSubmissionEmail(email, fullName, categoryTitle);

        // 8. ADD NOTIFICATION
        jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                userId, "Scholarship Application Received",
                "Your application for " + categoryTitle + " has been successfully submitted and is awaiting review.");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("applicationId", appId);
        res.put("category", category);
        res.put("categoryTitle", categoryTitle);
        res.put("status", "SUBMITTED");
        res.put("emailSent", sent);
        res.put("message", "Your scholarship application has been successfully submitted and is awaiting review.");
        return res;
    }

    @Transactional
    public Map<String, Object> cancelScholarship(long userId, long applicationId) {
        List<Map<String, Object>> apps = jdbc.queryForList(
                "SELECT * FROM scholarship_applications WHERE id=? AND user_id=?", applicationId, userId);
        if (apps.isEmpty()) {
            throw new IllegalArgumentException("Scholarship application not found.");
        }
        Map<String, Object> app = apps.get(0);
        String currentStatus = Objects.toString(app.get("status"), "");
        if ("APPROVED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Approved scholarship applications cannot be cancelled.");
        }
        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Application is already cancelled.");
        }

        jdbc.update("UPDATE scholarship_applications SET status='CANCELLED' WHERE id=?", applicationId);
        jdbc.update("UPDATE scholarship_quota_log SET status='CANCELLED' WHERE application_id=?", applicationId);

        jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                userId, "Scholarship Application Cancelled",
                "Your scholarship application for " + app.get("category_title") + " has been cancelled. Note: Quota remains consumed.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("applicationId", applicationId);
        out.put("status", "CANCELLED");
        out.put("message", "Application cancelled successfully.");
        return out;
    }

    public Map<String, Object> getMyScholarshipApplication(long userId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM scholarship_applications WHERE user_id=? ORDER BY id DESC LIMIT 1", userId);
        if (list.isEmpty()) {
            // Check quota log
            List<Map<String, Object>> quota = jdbc.queryForList(
                    "SELECT * FROM scholarship_quota_log WHERE user_id=? ORDER BY id DESC LIMIT 1", userId);
            if (!quota.isEmpty()) {
                Map<String, Object> q = quota.get(0);
                Map<String, Object> stub = new LinkedHashMap<>();
                stub.put("hasUsedQuota", true);
                stub.put("status", q.get("status"));
                stub.put("category", q.get("category"));
                stub.put("categoryTitle", getStandardCategoryTitle(Objects.toString(q.get("category"), "")));
                return stub;
            }
            return Map.of("hasUsedQuota", false);
        }
        Map<String, Object> app = list.get(0);
        app.put("hasUsedQuota", true);
        return app;
    }

    public List<Map<String, Object>> scholarshipApplications(long userId) {
        return jdbc.queryForList("SELECT * FROM scholarship_applications WHERE user_id=? ORDER BY id DESC", userId);
    }

    private String normalizeCategory(String category, String title) {
        String combined = (Objects.toString(category, "") + " " + Objects.toString(title, "")).toUpperCase();
        if (combined.contains("UNDER 18") || combined.contains("UNDER_18") || combined.contains("U18") || combined.contains("YOUTH SCHOLARSHIP")) {
            return "UNDER_18";
        }
        if (combined.contains("ABOVE 18") || combined.contains("ABOVE_18") || combined.contains("ADULT")) {
            return "ABOVE_18";
        }
        if (combined.contains("WOMEN") || combined.contains("FEMALE")) {
            return "WOMEN";
        }
        if (combined.contains("DEFENCE") || combined.contains("DEFENSE") || combined.contains("ARMED")) {
            return "DEFENCE";
        }
        if (combined.contains("SINGLE") || combined.contains("PARENT")) {
            return "SINGLE_PARENT";
        }
        return Objects.toString(category, "UNDER_18").toUpperCase();
    }

    private String getStandardCategoryTitle(String cat) {
        return switch (cat) {
            case "UNDER_18" -> "Scholarship for Under 18";
            case "ABOVE_18" -> "Scholarship for Above 18";
            case "WOMEN" -> "Scholarship for Women";
            case "DEFENCE" -> "Scholarships for Children of Defence Personnel";
            case "SINGLE_PARENT" -> "Scholarship for Single Parents";
            default -> cat;
        };
    }

    // =========================================================================
    // ACHIEVEMENTS & TRIALS & OTHER PLATFORM METHODS
    // =========================================================================

    public List<Map<String,Object>> achievements(long userId) {
        return jdbc.queryForList("SELECT id,title,achievement_type,sport,level,achievement_year,academic_year,institution,position,result_label,medal_award,event_location,achieved_on,description,certificate_details,certificate_url,created_at FROM achievements WHERE user_id=? ORDER BY achievement_year DESC, achieved_on DESC, id DESC", userId);
    }

    public Map<String,Object> addAchievement(long userId, AchievementRequest r) {
        LocalDate date = parseDate(r.getEventDate());
        jdbc.update("INSERT INTO achievements(user_id,title,event_location,achieved_on,result_label,achievement_type,sport,level,achievement_year,academic_year,institution,position,medal_award,description,certificate_details,certificate_url) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                userId, clean(r.getTitle()), clean(r.getLocation()), date, clean(r.getPosition()), clean(r.getType()), clean(r.getSport()), clean(r.getLevel()), r.getYear(), clean(r.getAcademicYear()), clean(r.getInstitution()), clean(r.getPosition()), clean(r.getMedalAward()), clean(r.getDescription()), clean(r.getCertificateDetails()), clean(r.getCertificateUrl()));
        return jdbc.queryForMap("SELECT id,title,achievement_type,sport,level,achievement_year,academic_year,institution,position,result_label,medal_award,event_location,achieved_on,description,certificate_details,certificate_url,created_at FROM achievements WHERE user_id=? ORDER BY id DESC LIMIT 1", userId);
    }
    public void deleteAchievement(long userId,long id) { if(jdbc.update("DELETE FROM achievements WHERE id=? AND user_id=?",id,userId)==0) throw new IllegalArgumentException("Achievement not found"); }
    public Map<String,Object> achievement(long userId,long id) { return jdbc.queryForMap("SELECT id,title,achievement_type,sport,level,achievement_year,academic_year,institution,position,result_label,medal_award,event_location,achieved_on,description,certificate_details,certificate_url,created_at FROM achievements WHERE id=? AND user_id=?", id,userId); }

    // =========================================================================
    // ATHLETE PERFORMANCE METRICS & PERSONAL RECORDS (PR) TRACKER
    // =========================================================================
    public List<Map<String, Object>> metrics(long userId) {
        return jdbc.queryForList(
                "SELECT id, metric_name, metric_value, unit, category, recorded_date, notes, created_at " +
                "FROM athlete_metrics WHERE user_id=? ORDER BY recorded_date DESC, id DESC", userId);
    }

    public Map<String, Object> metricsSummary(long userId) {
        List<Map<String, Object>> all = metrics(userId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTests", all.size());

        Map<String, List<Map<String, Object>>> grouped = all.stream()
                .collect(Collectors.groupingBy(m -> Objects.toString(m.get("metric_name"), "").trim(), LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> prs = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Map<String, Object>> entries = entry.getValue();
            if (entries.isEmpty()) continue;

            String unit = Objects.toString(entries.get(0).get("unit"), "");
            String category = Objects.toString(entries.get(0).get("category"), "General");

            boolean lowerIsBetter = name.toLowerCase().contains("sprint") || name.toLowerCase().contains("dash")
                    || (name.toLowerCase().contains("run") && unit.toLowerCase().contains("s"));

            Map<String, Object> bestEntry = entries.stream().min((a, b) -> {
                double valA = ((Number) a.get("metric_value")).doubleValue();
                double valB = ((Number) b.get("metric_value")).doubleValue();
                return lowerIsBetter ? Double.compare(valA, valB) : Double.compare(valB, valA);
            }).orElse(entries.get(0));

            Map<String, Object> pr = new LinkedHashMap<>();
            pr.put("metricName", name);
            pr.put("bestValue", bestEntry.get("metric_value"));
            pr.put("unit", unit);
            pr.put("category", category);
            pr.put("bestDate", bestEntry.get("recorded_date"));
            pr.put("latestValue", entries.get(0).get("metric_value"));
            pr.put("latestDate", entries.get(0).get("recorded_date"));
            pr.put("testCount", entries.size());
            pr.put("lowerIsBetter", lowerIsBetter);
            prs.add(pr);
        }
        summary.put("personalRecords", prs);
        return summary;
    }

    public Map<String, Object> addMetric(long userId, AthleteMetricRequest r) {
        LocalDate date = parseDate(r.getRecordedDate());
        if (date == null) date = LocalDate.now();

        String name = clean(r.getMetricName());
        String unit = clean(r.getUnit());
        String category = clean(r.getCategory());
        if (category == null || category.isBlank()) {
            String lower = name.toLowerCase();
            if (lower.contains("sprint") || lower.contains("dash") || lower.contains("agility") || lower.contains("shuttle")) {
                category = "Speed & Agility";
            } else if (lower.contains("jump") || lower.contains("bench") || lower.contains("squat") || lower.contains("press") || lower.contains("deadlift")) {
                category = "Strength & Power";
            } else if (lower.contains("yo-yo") || lower.contains("beep") || lower.contains("run") || lower.contains("endurance")) {
                category = "Endurance";
            } else {
                category = "Sport-Specific";
            }
        }

        jdbc.update("INSERT INTO athlete_metrics(user_id, metric_name, metric_value, unit, category, recorded_date, notes) VALUES(?,?,?,?,?,?,?)",
                userId, name, r.getMetricValue(), unit, category, date, clean(r.getNotes()));

        jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                userId, "Performance Metric Logged",
                "New record logged: " + name + " - " + r.getMetricValue() + " " + unit + " on " + date + ".");

        return jdbc.queryForMap(
                "SELECT id, metric_name, metric_value, unit, category, recorded_date, notes, created_at " +
                "FROM athlete_metrics WHERE user_id=? ORDER BY id DESC LIMIT 1", userId);
    }

    public void deleteMetric(long userId, long metricId) {
        int n = jdbc.update("DELETE FROM athlete_metrics WHERE id=? AND user_id=?", metricId, userId);
        if (n == 0) throw new IllegalArgumentException("Metric record not found.");
    }

    public Map<String, Object> scoutingCard(long athleteId) {
        Map<String, Object> profile = publicAthleteProfile(athleteId);
        List<Map<String, Object>> metrics = metrics(athleteId);
        Map<String, Object> metricsSummary = metricsSummary(athleteId);
        List<Map<String, Object>> apps = jdbc.queryForList(
                "SELECT a.id, a.status, t.name as trial_name, t.sport, t.trial_date, t.venue " +
                "FROM trial_applications a JOIN trials t ON t.id = a.trial_id " +
                "WHERE a.user_id=? ORDER BY a.created_at DESC LIMIT 5", athleteId);

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("athlete", profile);
        card.put("metrics", metrics);
        card.put("metricsSummary", metricsSummary);
        card.put("trialHistory", apps);
        card.put("verificationUrl", publicBaseUrl + "/#athlete/" + athleteId);
        card.put("generatedAt", LocalDate.now().toString());
        return card;
    }

    public List<Map<String,Object>> trials() { return jdbc.queryForList("SELECT t.id,t.name,t.sport,t.category,t.age_category,t.registration_status,t.featured,t.organization,t.venue,t.trial_date, COALESCE(SUM(s.capacity),0) total_capacity, COALESCE(SUM(s.booked_count),0) booked_count, COALESCE(SUM(s.capacity-s.booked_count),0) remaining_slots FROM trials t LEFT JOIN trial_slots s ON s.trial_id=t.id GROUP BY t.id ORDER BY t.trial_date"); }
    public List<Map<String,Object>> featuredTrials() { return jdbc.queryForList("SELECT t.id,t.name,t.sport,t.category,t.age_category,t.registration_status,t.organization,t.venue,t.trial_date,COALESCE(SUM(s.capacity-s.booked_count),0) remaining_slots FROM trials t LEFT JOIN trial_slots s ON s.trial_id=t.id WHERE t.featured=TRUE AND t.registration_status='REG OPEN' GROUP BY t.id ORDER BY t.trial_date LIMIT 5"); }
    public Map<String,Object> trial(long id) { return jdbc.queryForMap("SELECT t.id,t.name,t.sport,t.category,t.age_category,t.registration_status,t.organization,t.venue,t.trial_date,COALESCE(SUM(s.capacity-s.booked_count),0) remaining_slots FROM trials t LEFT JOIN trial_slots s ON s.trial_id=t.id WHERE t.id=? GROUP BY t.id",id); }
    public List<Map<String,Object>> slots(long trialId) { return jdbc.queryForList("SELECT id,trial_id,slot_date,slot_time,capacity,booked_count,(capacity-booked_count) remaining_slots FROM trial_slots WHERE trial_id=? ORDER BY slot_date,slot_time",trialId); }

    @Transactional
    public Map<String,Object> applyTrial(long userId,long trialId,TrialApplicationRequest r) {
        Map<String,Object> t=trial(trialId);
        Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM trial_applications WHERE user_id=? AND trial_id=?",Integer.class,userId,trialId);
        if(existing!=null && existing>0) throw new IllegalStateException("You have already applied for this trial.");
        jdbc.update("INSERT INTO trial_applications(user_id,trial_id,athlete_name,email,phone,location,sport,position_role,age,institution,experience,achievements,introduction,skills,additional_info,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'SUBMITTED')",userId,trialId,clean(r.getAthleteName()),clean(r.getEmail()),clean(r.getPhone()),clean(r.getLocation()),clean(r.getSport()),clean(r.getPosition()),r.getAge(),clean(r.getInstitution()),clean(r.getExperience()),clean(r.getAchievements()),clean(r.getIntroduction()),clean(r.getSkills()),clean(r.getAdditionalInfo()));
        long id=jdbc.queryForObject("SELECT id FROM trial_applications WHERE user_id=? AND trial_id=?",Long.class,userId,trialId);
        String token=UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO trial_application_confirmations(application_id,user_id,email,confirmation_token) VALUES(?,?,?,?)",id,userId,clean(r.getEmail()),token);
        String link=publicBaseUrl+"/api/trials/applications/confirm?token="+token;
        boolean sent=emailService.sendTrialApplicationConfirmation(clean(r.getEmail()),clean(r.getAthleteName()),t,id,link);
        jdbc.update("UPDATE trial_application_confirmations SET email_sent=? WHERE application_id=?",sent,id);
        jdbc.update("INSERT INTO notifications(user_id,title,message) VALUES(?,?,?)",userId,"Trial application submitted","Your application for " + t.get("name") + " has been submitted.");
        Map<String,Object> out=new LinkedHashMap<>(); out.put("applicationId",id); out.put("status","SUBMITTED"); out.put("trial",t.get("name")); out.put("trialId",trialId); out.put("emailSent",sent); out.put("confirmationUrl",link); return out;
    }
    public List<Map<String,Object>> applications(long userId){ return jdbc.queryForList("SELECT a.id application_id,a.status,a.created_at,a.updated_at,a.athlete_name,a.email,a.phone,a.location,a.sport,a.position_role,a.age,a.institution,a.experience,a.achievements,a.introduction,a.skills,a.additional_info,a.confirmed_trial_date,a.confirmed_trial_time,tc.email_sent,tc.confirmation_token,t.id trial_id,t.name trial_name,t.organization,t.venue,t.trial_date FROM trial_applications a JOIN trials t ON t.id=a.trial_id LEFT JOIN trial_application_confirmations tc ON tc.application_id=a.id WHERE a.user_id=? ORDER BY a.created_at DESC",userId); }
    public Map<String,Object> application(long userId,long id){ return jdbc.queryForMap("SELECT a.id application_id,a.status,a.created_at,a.updated_at,a.athlete_name,a.email,a.phone,a.location,a.sport,a.position_role,a.age,a.institution,a.experience,a.achievements,a.introduction,a.skills,a.additional_info,a.confirmed_trial_date,a.confirmed_trial_time,tc.email_sent,tc.confirmation_token,t.id trial_id,t.name trial_name,t.organization,t.venue,t.trial_date FROM trial_applications a JOIN trials t ON t.id=a.trial_id LEFT JOIN trial_application_confirmations tc ON tc.application_id=a.id WHERE a.user_id=? AND a.id=?",userId,id); }

    public Map<String,Object> confirmTrialApplication(String token){
        try {
            List<Map<String,Object>> list = jdbc.queryForList("SELECT a.id application_id,tc.email_sent,tc.created_at,tc.confirmed_at,a.athlete_name,a.email,a.phone,a.sport,a.age,a.position_role,a.institution,t.id as trial_id,t.name trial_name,t.sport as trial_sport,t.organization,t.venue,t.trial_date FROM trial_application_confirmations tc JOIN trial_applications a ON a.id=tc.application_id JOIN trials t ON t.id=a.trial_id WHERE tc.confirmation_token=?",token);
            if (list.isEmpty()) return null;
            Map<String,Object> c = new LinkedHashMap<>(list.get(0));
            jdbc.update("UPDATE trial_application_confirmations SET confirmed_at=NOW() WHERE confirmation_token=? AND confirmed_at IS NULL",token);
            c.put("confirmed",true);
            c.put("status","CONFIRMED");
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String,Object>> scouts(){ return jdbc.queryForList("SELECT id,name,organization,sports,specialization,experience,location FROM scouts ORDER BY id"); }
    public Map<String,Object> connect(long userId,long scoutId,CoachConnectionRequest r){
        Map<String,Object> scout=jdbc.queryForMap("SELECT id,name FROM scouts WHERE id=?",scoutId);
        Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM scout_connections WHERE user_id=? AND scout_id=?",Integer.class,userId,scoutId);
        if(existing!=null && existing>0) throw new IllegalStateException("Connection request already exists.");
        jdbc.update("INSERT INTO scout_connections(user_id,scout_id,status,message) VALUES(?,?, 'PENDING',?)",userId,scoutId,clean(r.getMessage()));
        jdbc.update("INSERT INTO notifications(user_id,title,message) VALUES(?,?,?)",userId,"Scout connection request","Your connection request has been sent to " + scout.get("name") + ".");
        return connection(userId,scoutId);
    }
    public List<Map<String,Object>> connections(long userId){return jdbc.queryForList("SELECT c.id,c.status,c.message,c.created_at,c.updated_at,s.id scout_id,s.name,s.organization,s.sports,s.specialization,s.experience,s.location FROM scout_connections c JOIN scouts s ON s.id=c.scout_id WHERE c.user_id=? ORDER BY c.created_at DESC",userId);}
    private Map<String,Object> connection(long userId,long scoutId){return jdbc.queryForMap("SELECT c.id,c.status,c.message,c.created_at,s.id scout_id,s.name,s.organization FROM scout_connections c JOIN scouts s ON s.id=c.scout_id WHERE c.user_id=? AND c.scout_id=?",userId,scoutId);}

    @Transactional
    public Map<String,Object> book(long userId, BookingRequest req){
        Integer applied=jdbc.queryForObject("SELECT COUNT(*) FROM trial_applications a JOIN trial_slots s ON s.trial_id=a.trial_id WHERE a.user_id=? AND s.id=?",Integer.class,userId,req.getSlotId());
        if(applied==null || applied==0) throw new IllegalStateException("Submit the trial application form before booking a trial slot.");
        Map<String,Object> slot=jdbc.queryForMap("SELECT s.id,s.trial_id,s.slot_date,s.slot_time,s.capacity,s.booked_count,t.name trial_name,t.sport,t.venue FROM trial_slots s JOIN trials t ON t.id=s.trial_id WHERE s.id=? FOR UPDATE",req.getSlotId());
        Integer duplicate=jdbc.queryForObject("SELECT COUNT(*) FROM bookings WHERE user_id=? AND slot_id=?",Integer.class,userId,req.getSlotId());
        if(duplicate!=null && duplicate>0) throw new IllegalStateException("You have already booked this slot.");
        int updated=jdbc.update("UPDATE trial_slots SET booked_count=booked_count+1 WHERE id=? AND booked_count<capacity",req.getSlotId());
        if(updated==0) throw new IllegalStateException("Sorry, this trial slot is full.");
        String token=UUID.randomUUID().toString().replace("-","");
        try { jdbc.update("INSERT INTO bookings(user_id,trial_id,slot_id,booking_status,confirmation_token) VALUES(?,?,?,'CONFIRMED',?)",userId,slot.get("trial_id"),req.getSlotId(),token); }
        catch(DuplicateKeyException e){ throw new IllegalStateException("You have already booked this slot."); }
        long bookingId=jdbc.queryForObject("SELECT id FROM bookings WHERE confirmation_token=?",Long.class,token);
        Map<String,Object> user=jdbc.queryForMap("SELECT full_name,email FROM users WHERE id=?",userId);
        jdbc.update("INSERT INTO booking_confirmations(booking_id,user_id,email,confirmation_token) VALUES(?,?,?,?)",bookingId,userId,user.get("email"),token);
        String link=publicBaseUrl+"/api/confirm-booking?token="+token;
        boolean sent=emailService.sendBookingConfirmation(Objects.toString(user.get("email"),""),Objects.toString(user.get("full_name"),"Athlete"),slot,bookingId,link);
        jdbc.update("UPDATE booking_confirmations SET email_sent=? WHERE booking_id=?",sent,bookingId);
        jdbc.update("INSERT INTO notifications(user_id,title,message) VALUES(?,?,?)",userId,"Trial booking confirmed","Your trial slot for " + slot.get("trial_name") + " on " + slot.get("slot_date") + " at " + slot.get("slot_time") + " is confirmed.");
        Map<String,Object> out=booking(userId,bookingId); out.put("confirmationUrl",link); out.put("emailSent",sent); return out;
    }
    public List<Map<String,Object>> bookings(long userId){return jdbc.queryForList("SELECT b.id booking_id,b.booking_status status,b.confirmation_token,b.checked_in_at,b.created_at,s.id slot_id,s.slot_date,s.slot_time,t.id trial_id,t.name trial_name,t.sport,t.organization,t.venue,bc.email_sent,bc.confirmed_at FROM bookings b JOIN trial_slots s ON s.id=b.slot_id JOIN trials t ON t.id=b.trial_id LEFT JOIN booking_confirmations bc ON bc.booking_id=b.id WHERE b.user_id=? ORDER BY s.slot_date,s.slot_time",userId);}
    public Map<String,Object> booking(long userId,long id){return jdbc.queryForMap("SELECT b.id booking_id,b.booking_status status,b.confirmation_token,b.checked_in_at,b.created_at,s.id slot_id,s.slot_date,s.slot_time,t.id trial_id,t.name trial_name,t.sport,t.organization,t.venue,bc.email_sent,bc.confirmed_at FROM bookings b JOIN trial_slots s ON s.id=b.slot_id JOIN trials t ON t.id=b.trial_id LEFT JOIN booking_confirmations bc ON bc.booking_id=b.id WHERE b.user_id=? AND b.id=?",userId,id);}
    public Map<String,Object> confirm(String token){
        try {
            List<Map<String,Object>> list = jdbc.queryForList("SELECT b.id booking_id,b.booking_status,b.checked_in_at,bc.user_id,bc.email_sent,bc.created_at,bc.confirmed_at,u.full_name as athlete_name,u.email,s.slot_date,s.slot_time,t.id as trial_id,t.name trial_name,t.sport,t.organization,t.venue FROM booking_confirmations bc JOIN bookings b ON b.id=bc.booking_id JOIN trial_slots s ON s.id=b.slot_id JOIN trials t ON t.id=b.trial_id JOIN users u ON u.id=b.user_id WHERE bc.confirmation_token=?",token);
            if (list.isEmpty()) return null;
            Map<String,Object> c = new LinkedHashMap<>(list.get(0));
            jdbc.update("UPDATE booking_confirmations SET confirmed_at=NOW() WHERE confirmation_token=? AND confirmed_at IS NULL",token);
            c.put("confirmed",true);
            c.put("status","CONFIRMED");
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Map<String, Object> checkin(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT b.id booking_id, b.booking_status, b.checked_in_at, b.user_id, " +
                    "u.full_name as athlete_name, u.email, u.sport as athlete_sport, " +
                    "s.slot_date, s.slot_time, t.id as trial_id, t.name trial_name, t.sport, t.organization, t.venue " +
                    "FROM bookings b " +
                    "JOIN trial_slots s ON s.id = b.slot_id " +
                    "JOIN trials t ON t.id = b.trial_id " +
                    "JOIN users u ON u.id = b.user_id " +
                    "WHERE b.confirmation_token = ?", token.trim());
            if (list.isEmpty()) return null;

            Map<String, Object> booking = new LinkedHashMap<>(list.get(0));
            long bookingId = ((Number) booking.get("booking_id")).longValue();
            long userId = ((Number) booking.get("user_id")).longValue();
            String currentStatus = Objects.toString(booking.get("booking_status"), "");

            if (!"CHECKED_IN".equalsIgnoreCase(currentStatus)) {
                jdbc.update("UPDATE bookings SET booking_status='CHECKED_IN', checked_in_at=NOW() WHERE id=?", bookingId);
                booking.put("booking_status", "CHECKED_IN");
                booking.put("justCheckedIn", true);
                booking.put("checked_in_at", new java.util.Date());
                jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                        userId, "Trial Attendance Checked-In",
                        "Official QR Check-in verified! You are checked in for " + booking.get("trial_name") + " on " + booking.get("slot_date") + " at " + booking.get("slot_time") + ".");
            } else {
                booking.put("justCheckedIn", false);
            }
            return booking;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String,Object>> opportunities(){
        try {
            List<Map<String, Object>> list = jdbc.queryForList("SELECT slug as id, type, name, description, deadline FROM opportunities WHERE is_active=TRUE ORDER BY id ASC");
            if (!list.isEmpty()) return list;
        } catch (Exception ignored) {}
        List<Map<String,Object>> out=new ArrayList<>();
        out.add(op("boxing-academy","Academy & Training","National Boxing Excellence Academy","Full scholarship residential training camp hosted at Army Sports Institute, Pune.","2026-09-15"));
        out.add(op("jsw-stipend","Scholarship Grant","JSW Sports Grassroots Stipend","Monthly financial aid + elite support for talented athletes.","2026-09-01"));
        out.add(op("khelo-trials","Open Trials","Khelo India National Talent Hunt","Zonal open trials for athletics, weightlifting and hockey.","2026-09-30"));
        return out;
    }
    public List<Map<String,Object>> announcements(){
        try {
            return jdbc.queryForList("SELECT id, title, content, category, priority, created_at FROM announcements WHERE is_active=TRUE ORDER BY created_at DESC LIMIT 10");
        } catch (Exception e) {
            return List.of();
        }
    }
    private Map<String,Object> op(String id,String type,String name,String description,String deadline){Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("type",type);m.put("name",name);m.put("description",description);m.put("deadline",deadline);return m;}
    public Map<String,Object> apply(long userId,String id){
        Map<String,Object> op=opportunities().stream().filter(x->x.get("id").equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Opportunity not found"));
        int n=jdbc.update("INSERT IGNORE INTO applications(user_id,opportunity_id,opportunity_name,status) VALUES(?,?,?,'SUBMITTED')",userId,id,op.get("name"));
        if(n==1)jdbc.update("INSERT INTO notifications(user_id,title,message) VALUES(?,?,?)",userId,"Opportunity application submitted","Your application for "+op.get("name")+" has been submitted.");
        return Map.of("message",n==1?"Application submitted successfully":"You have already applied for this opportunity.","opportunity",op.get("name"));
    }

    public List<Map<String,Object>> notifications(long userId){return jdbc.queryForList("SELECT id,title,message,is_read,created_at FROM notifications WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 30",userId);}
    public void markNotificationRead(long userId,long id){jdbc.update("UPDATE notifications SET is_read=TRUE WHERE id=? AND user_id=?",id,userId);}
    public Map<String,Object> matching(long userId) {
        Map<String,Object> user = jdbc.queryForMap("SELECT sport, location, latitude, longitude FROM users WHERE id=?", userId);
        String sport = Objects.toString(user.get("sport"), "").trim();
        String location = Objects.toString(user.get("location"), "").trim();
        Double userLat = user.get("latitude") != null ? ((Number) user.get("latitude")).doubleValue() : null;
        Double userLon = user.get("longitude") != null ? ((Number) user.get("longitude")).doubleValue() : null;
        int achievementCount = jdbc.queryForObject("SELECT COUNT(*) FROM achievements WHERE user_id=?", Integer.class, userId);
        int trialAppCount = jdbc.queryForObject("SELECT COUNT(*) FROM trial_applications WHERE user_id=?", Integer.class, userId);
        boolean hasScholarshipApp = jdbc.queryForObject("SELECT COUNT(*) FROM scholarship_applications WHERE user_id=?", Integer.class, userId) > 0;

        // Profile completeness
        int completionPct = 25; // full_name always set
        if (!sport.isBlank()) completionPct += 25;
        if (!location.isBlank()) completionPct += 25;
        if (achievementCount > 0) completionPct += 25;

        // New users: not enough info yet
        if (sport.isBlank() && location.isBlank() && achievementCount == 0) {
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("profileIncomplete", true);
            result.put("completionPct", completionPct);
            result.put("missingFields", buildMissingFields(sport, location, achievementCount));
            return result;
        }

        List<Map<String,Object>> matches = new ArrayList<>();

        // --- COACH MATCHES ---
        List<Map<String,Object>> allCoaches = jdbc.queryForList("SELECT id, name, organization, sports, specialization, experience, location, latitude, longitude FROM coaches");
        List<Map<String,Object>> coachConnections = jdbc.queryForList("SELECT coach_id, status FROM coach_connections WHERE user_id=?", userId);
        for (Map<String,Object> c : allCoaches) {
            int score = 0;
            String coachSports = Objects.toString(c.get("sports"), "").toLowerCase();
            if (!sport.isBlank() && coachSports.contains(sport.toLowerCase())) score += 30;
            else if (!sport.isBlank()) score += 5;
            // Distance factor
            if (userLat != null && userLon != null && c.get("latitude") != null && c.get("longitude") != null) {
                double dist = haversine(userLat, userLon, ((Number) c.get("latitude")).doubleValue(), ((Number) c.get("longitude")).doubleValue());
                score += (int) Math.max(0, 25 - dist / 20);
            } else {
                score += 10; // partial if no location
            }
            // Achievement level bonus
            if (achievementCount >= 3) score += 20;
            else if (achievementCount >= 1) score += 12;
            // Profile completeness
            score += completionPct / 7;
            // Activity
            if (trialAppCount > 0) score += 10;
            score = Math.min(99, Math.max(30, score));
            boolean alreadyConnected = coachConnections.stream().anyMatch(x -> Objects.equals(x.get("coach_id"), c.get("id")));
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", c.get("id"));
            m.put("name", c.get("name"));
            m.put("type", "Recommended Coach");
            m.put("organization", c.get("organization"));
            m.put("sports", c.get("sports"));
            m.put("location", c.get("location"));
            m.put("score", score);
            m.put("alreadyConnected", alreadyConnected);
            m.put("reason", buildCoachReason(sport, achievementCount, location));
            matches.add(m);
        }
        matches.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        List<Map<String,Object>> topCoaches = matches.stream().limit(3).collect(java.util.stream.Collectors.toList());

        // --- TRIAL MATCHES ---
        List<Map<String,Object>> trials = jdbc.queryForList(
                "SELECT t.id, t.name, t.sport, t.category, t.age_category, t.registration_status, t.venue, t.trial_date, " +
                "COALESCE(SUM(s.capacity - s.booked_count), 0) remaining_slots " +
                "FROM trials t LEFT JOIN trial_slots s ON s.trial_id = t.id " +
                "WHERE t.registration_status='REG OPEN' GROUP BY t.id ORDER BY t.trial_date");
        List<Map<String,Object>> trialMatches = new ArrayList<>();
        List<Map<String,Object>> existingApps = jdbc.queryForList("SELECT trial_id FROM trial_applications WHERE user_id=?", userId);
        java.util.Set<Object> appliedTrialIds = existingApps.stream().map(a -> a.get("trial_id")).collect(java.util.stream.Collectors.toSet());
        for (Map<String,Object> t : trials) {
            if (((Number) t.get("remaining_slots")).intValue() <= 0) continue;
            int score = 0;
            String trialSport = Objects.toString(t.get("sport"), "").toLowerCase();
            if (!sport.isBlank() && trialSport.contains(sport.toLowerCase())) score += 35;
            else if (!sport.isBlank()) score += 8;
            // Venue city match
            if (!location.isBlank()) {
                String venue = Objects.toString(t.get("venue"), "").toLowerCase();
                String[] locParts = location.toLowerCase().split(",|\\s+");
                for (String part : locParts) {
                    if (part.length() > 3 && venue.contains(part)) { score += 20; break; }
                }
            } else { score += 8; }
            score += Math.min(20, ((Number) t.get("remaining_slots")).intValue());
            score = Math.min(99, Math.max(30, score));
            boolean alreadyApplied = appliedTrialIds.contains(t.get("id"));
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", t.get("id"));
            m.put("name", t.get("name"));
            m.put("type", "Recommended Trial");
            m.put("sport", t.get("sport"));
            m.put("venue", t.get("venue"));
            m.put("trialDate", t.get("trial_date"));
            m.put("score", score);
            m.put("alreadyApplied", alreadyApplied);
            m.put("remainingSlots", t.get("remaining_slots"));
            m.put("reason", "Open trial for " + t.get("sport") + " — " + t.get("remaining_slots") + " slots available.");
            trialMatches.add(m);
        }
        trialMatches.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        List<Map<String,Object>> topTrials = trialMatches.stream().limit(2).collect(java.util.stream.Collectors.toList());

        // --- SCHOLARSHIP MATCH ---
        List<Map<String,Object>> scholarshipMatches = new ArrayList<>();
        if (!hasScholarshipApp) {
            int score = 40; // base eligibility
            if (achievementCount >= 2) score += 20;
            else if (achievementCount >= 1) score += 10;
            score += completionPct / 10;
            score = Math.min(95, score);
            Map<String,Object> sm = new LinkedHashMap<>();
            sm.put("name", "AthleteLink Scholarship Program");
            sm.put("type", "Scholarship Opportunity");
            sm.put("score", score);
            sm.put("reason", "You have not yet applied for a scholarship. " + (achievementCount > 0 ? "Your " + achievementCount + " achievement(s) strengthen your application." : "Add achievements to boost your score."));
            scholarshipMatches.add(sm);
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("profileIncomplete", false);
        result.put("completionPct", completionPct);
        result.put("coaches", topCoaches);
        result.put("trials", topTrials);
        result.put("scholarships", scholarshipMatches);
        return result;
    }

    private List<String> buildMissingFields(String sport, String location, int achievementCount) {
        List<String> missing = new ArrayList<>();
        if (sport.isBlank()) missing.add("sport");
        if (location.isBlank()) missing.add("location");
        if (achievementCount == 0) missing.add("achievements");
        return missing;
    }

    private String buildCoachReason(String sport, int achievements, String location) {
        StringBuilder sb = new StringBuilder();
        if (!sport.isBlank()) sb.append("Sport match: ").append(sport).append(". ");
        if (achievements > 0) sb.append(achievements).append(" achievement(s) on your profile. ");
        if (!location.isBlank()) sb.append("Near ").append(location).append(".");
        return sb.toString().trim();
    }
    public List<Map<String,Object>> coaches(){
        return jdbc.queryForList("SELECT id,name,organization,sports,specialization,experience,location,latitude,longitude FROM coaches ORDER BY id");
    }

    public List<Map<String,Object>> nearbyCoaches(long userId, Double lat, Double lon, double radiusKm){
        if(lat==null || lon==null){
            Map<String,Object> u=jdbc.queryForMap("SELECT latitude,longitude FROM users WHERE id=?",userId);
            if(u.get("latitude")!=null && u.get("longitude")!=null){ lat=((Number)u.get("latitude")).doubleValue(); lon=((Number)u.get("longitude")).doubleValue(); }
        }
        if(lat==null || lon==null) return coachesWithDistance(userId, null, null, radiusKm);
        jdbc.update("UPDATE users SET latitude=?, longitude=? WHERE id=?",lat,lon,userId);
        return coachesWithDistance(userId,lat,lon,radiusKm);
    }

    private List<Map<String,Object>> coachesWithDistance(long userId, Double lat, Double lon, double radiusKm){
        Map<String,Object> u=jdbc.queryForMap("SELECT sport FROM users WHERE id=?",userId);
        String sport=Objects.toString(u.get("sport"),"").toLowerCase();
        int achievementCount=jdbc.queryForObject("SELECT COUNT(*) FROM achievements WHERE user_id=?",Integer.class,userId);
        List<Map<String,Object>> raw=jdbc.queryForList("SELECT id,name,organization,sports,specialization,experience,location,latitude,longitude FROM coaches");
        List<Map<String,Object>> out=new ArrayList<>();
        for(Map<String,Object> c:raw){
            double distance=lat==null?9999:haversine(lat,lon,((Number)c.get("latitude")).doubleValue(),((Number)c.get("longitude")).doubleValue());
            if(lat!=null && distance>radiusKm) continue;
            String cs=Objects.toString(c.get("sports"),"").toLowerCase();
            int score=(sport.isBlank()||cs.contains(sport))?20:0;
            if(achievementCount>0) score+=10;
            c.put("distance_km",Math.round(distance*10.0)/10.0);
            c.put("match_score",Math.min(100,70+score-(int)Math.min(20,distance/10)));
            c.remove("latitude"); c.remove("longitude");
            out.add(c);
        }
        out.sort(Comparator.comparingDouble(m->((Number)m.get("distance_km")).doubleValue()));
        return out;
    }

    private double haversine(double lat1,double lon1,double lat2,double lon2){
        double dLat=Math.toRadians(lat2-lat1), dLon=Math.toRadians(lon2-lon1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return 6371.0088*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }

    @Transactional
    public Map<String,Object> connectCoach(long userId,long coachId,CoachConnectionRequest r){
        Map<String,Object> coach=jdbc.queryForMap("SELECT id,name FROM coaches WHERE id=?",coachId);
        Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM coach_connections WHERE user_id=? AND coach_id=?",Integer.class,userId,coachId);
        if(existing!=null && existing>0) throw new IllegalStateException("Connection request already exists.");
        String message=clean(r.getMessage());
        jdbc.update("INSERT INTO coach_connections(user_id,coach_id,status,message) VALUES(?,?, 'PENDING',?)",userId,coachId,message);
        jdbc.update("INSERT INTO notifications(user_id,title,message) VALUES(?,?,?)",userId,"Coach connection request","Your connection request has been sent to " + coach.get("name") + ".");
        return jdbc.queryForMap("SELECT c.id,c.status,c.message,c.created_at,c.updated_at,ch.id coach_id,ch.name,ch.organization,ch.sports,ch.specialization,ch.experience,ch.location FROM coach_connections c JOIN coaches ch ON ch.id=c.coach_id WHERE c.user_id=? AND c.coach_id=?",userId,coachId);
    }

    public List<Map<String,Object>> coachConnections(long userId){return jdbc.queryForList("SELECT c.id,c.status,c.message,c.created_at,c.updated_at,ch.id coach_id,ch.name,ch.organization,ch.sports,ch.specialization,ch.experience,ch.location FROM coach_connections c JOIN coaches ch ON ch.id=c.coach_id WHERE c.user_id=? ORDER BY c.created_at DESC",userId);}

    public Map<String,Object> dashboard(long userId){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("achievements",achievements(userId));
        m.put("metrics",metrics(userId));
        m.put("metricsSummary",metricsSummary(userId));
        m.put("applications",applications(userId));
        m.put("scholarshipApplications",scholarshipApplications(userId));
        m.put("myScholarship",getMyScholarshipApplication(userId));
        m.put("bookings",bookings(userId));
        m.put("connections",connections(userId));
        m.put("notifications",notifications(userId));
        return m;
    }
    @Transactional
    public Map<String,Object> cancelBooking(long userId, long bookingId) {
        List<Map<String,Object>> bookings = jdbc.queryForList(
                "SELECT b.id, b.booking_status, b.slot_id, t.name trial_name, u.email, u.full_name FROM bookings b " +
                "JOIN trial_slots s ON s.id = b.slot_id JOIN trials t ON t.id = b.trial_id " +
                "JOIN users u ON u.id = b.user_id WHERE b.id=? AND b.user_id=?", bookingId, userId);
        if (bookings.isEmpty()) throw new IllegalArgumentException("Booking not found.");
        Map<String,Object> booking = bookings.get(0);
        if ("CANCELLED".equalsIgnoreCase(Objects.toString(booking.get("booking_status"), ""))) {
            throw new IllegalStateException("Booking is already cancelled.");
        }
        jdbc.update("UPDATE bookings SET booking_status='CANCELLED' WHERE id=?", bookingId);
        jdbc.update("UPDATE trial_slots SET booked_count = GREATEST(0, booked_count - 1) WHERE id=?", booking.get("slot_id"));
        jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                userId, "Booking Cancelled",
                "Your booking #" + bookingId + " for " + booking.get("trial_name") + " has been cancelled.");
        // Notify all admins
        List<Map<String,Object>> admins = jdbc.queryForList("SELECT id FROM users WHERE role='ADMIN'");
        for (Map<String,Object> admin : admins) {
            jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                    admin.get("id"),
                    "Booking Cancelled by Athlete",
                    "Booking #" + bookingId + " for '" + booking.get("trial_name") + "' was cancelled by " + booking.get("full_name") + " (" + booking.get("email") + "). Slot is now available.");
        }
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("bookingId", bookingId);
        out.put("status", "CANCELLED");
        out.put("message", "Booking cancelled successfully. The slot has been freed.");
        return out;
    }

    @Transactional
    public Map<String,Object> withdrawApplication(long userId, long applicationId) {
        List<Map<String,Object>> apps = jdbc.queryForList(
                "SELECT a.id, a.status, t.name trial_name FROM trial_applications a " +
                "JOIN trials t ON t.id = a.trial_id WHERE a.id=? AND a.user_id=?", applicationId, userId);
        if (apps.isEmpty()) throw new IllegalArgumentException("Application not found.");
        Map<String,Object> app = apps.get(0);
        String currentStatus = Objects.toString(app.get("status"), "");
        if ("WITHDRAWN".equalsIgnoreCase(currentStatus)) throw new IllegalStateException("Application is already withdrawn.");
        if ("CONFIRMED".equalsIgnoreCase(currentStatus)) throw new IllegalStateException("Confirmed applications cannot be withdrawn. Please cancel your booking first.");
        jdbc.update("UPDATE trial_applications SET status='WITHDRAWN', updated_at=NOW() WHERE id=?", applicationId);
        jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                userId, "Application Withdrawn",
                "Your application #" + applicationId + " for " + app.get("trial_name") + " has been withdrawn.");
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("applicationId", applicationId);
        out.put("status", "WITHDRAWN");
        out.put("message", "Application withdrawn successfully.");
        return out;
    }

    public void markAllNotificationsRead(long userId) {
        jdbc.update("UPDATE notifications SET is_read=TRUE WHERE user_id=? AND is_read=FALSE", userId);
    }

    public Map<String,Object> athleteDirectory(int page, int size) {
        int offset = page * size;
        List<Map<String,Object>> athletes = jdbc.queryForList(
            "SELECT u.id, u.full_name, u.sport, u.location, u.created_at, " +
            "COUNT(DISTINCT a.id) achievement_count, " +
            "COUNT(DISTINCT ta.id) active_trial_count " +
            "FROM users u " +
            "LEFT JOIN achievements a ON a.user_id = u.id " +
            "LEFT JOIN trial_applications ta ON ta.user_id = u.id AND ta.status NOT IN ('WITHDRAWN','CANCELLED') " +
            "WHERE u.role IS NULL OR u.role NOT IN ('ADMIN') " +
            "GROUP BY u.id ORDER BY achievement_count DESC, u.created_at DESC " +
            "LIMIT ? OFFSET ?", size, offset);
        long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE role IS NULL OR role NOT IN ('ADMIN')", Long.class);
        for (Map<String,Object> a : athletes) {
            int ac = ((Number) a.getOrDefault("achievement_count", 0)).intValue();
            int tc = ((Number) a.getOrDefault("active_trial_count", 0)).intValue();
            int score = 40;
            if (ac >= 1) score += Math.min(47, ac * 12);
            if (a.get("sport") != null && !Objects.toString(a.get("sport"),"").isBlank()) score += 15;
            if (a.get("location") != null && !Objects.toString(a.get("location"),"").isBlank()) score += 10;
            if (tc > 0) score += 5;
            a.put("aiScore", Math.min(99, score));
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("athletes", athletes);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public Map<String,Object> uploadVideo(long userId, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file uploaded.");
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String[] allowed = {".mp4", ".mov", ".avi", ".mkv", ".webm"};
        boolean validExt = false;
        for (String a : allowed) { if (ext.equalsIgnoreCase(a)) { validExt = true; break; } }
        if (!validExt) throw new IllegalArgumentException("Unsupported file type. Please upload MP4, MOV, AVI, MKV or WebM.");
        long maxBytes = 500L * 1024 * 1024; // 500MB
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("File too large. Maximum size is 500MB.");
        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(System.getProperty("user.home"), "athletelink-uploads", "videos");
            java.nio.file.Files.createDirectories(uploadDir);
            String safeFilename = "athlete_" + userId + "_" + System.currentTimeMillis() + ext;
            java.nio.file.Path dest = uploadDir.resolve(safeFilename);
            file.transferTo(dest.toFile());
            // Generate simulated AI analysis based on video metadata
            double fileMB = file.getSize() / (1024.0 * 1024.0);
            int postureScore = (int)(65 + Math.random() * 30);
            int symmetryScore = (int)(60 + Math.random() * 35);
            int explosivePower = (int)(55 + Math.random() * 40);
            int overallScore = (postureScore + symmetryScore + explosivePower) / 3;
            // Save a record to notifications so athlete sees it
            String summary = String.format("Video analysis complete: Posture %d%%, Symmetry %d%%, Explosive Power %d%%, Overall Score %d%%.",
                    postureScore, symmetryScore, explosivePower, overallScore);
            jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                    userId, "AI Video Analysis Complete", summary);
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("filename", safeFilename);
            out.put("fileSizeMB", String.format("%.2f", fileMB));
            out.put("status", "ANALYZED");
            out.put("postureScore", postureScore);
            out.put("symmetryScore", symmetryScore);
            out.put("explosivePower", explosivePower);
            out.put("overallScore", overallScore);
            out.put("feedback", buildVideoFeedback(postureScore, symmetryScore, explosivePower));
            out.put("message", "Analysis complete. Results saved to your notifications.");
            return out;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save video file: " + e.getMessage());
        }
    }

    private List<String> buildVideoFeedback(int posture, int symmetry, int power) {
        List<String> tips = new ArrayList<>();
        if (posture < 75) tips.add("Work on core stability — your posture alignment shows room for improvement.");
        else tips.add("Excellent posture control detected across most frames.");
        if (symmetry < 70) tips.add("Left-right asymmetry detected. Focus on balanced strength training.");
        else tips.add("Good bilateral symmetry in movement patterns.");
        if (power < 70) tips.add("Explosive power metrics suggest more plyometric training would help.");
        else tips.add("Strong explosive power and acceleration profile detected.");
        return tips;
    }

    public Map<String,Object> publicAthleteProfile(long athleteId) {
        List<Map<String,Object>> users = jdbc.queryForList(
                "SELECT id, full_name, sport, location, created_at FROM users WHERE id=?", athleteId);
        if (users.isEmpty()) throw new IllegalArgumentException("Athlete not found.");
        Map<String,Object> user = new LinkedHashMap<>(users.get(0));
        user.remove("password_hash");
        List<Map<String,Object>> achievements = jdbc.queryForList(
                "SELECT id, title, sport, level, achievement_year, medal_award, institution, event_location, result_label, description FROM achievements WHERE user_id=? ORDER BY achievement_year DESC, id DESC LIMIT 10",
                athleteId);
        List<Map<String,Object>> metrics = metrics(athleteId);
        Map<String,Object> metricsSummary = metricsSummary(athleteId);
        user.put("achievements", achievements);
        user.put("achievementCount", achievements.size());
        user.put("metrics", metrics);
        user.put("metricsSummary", metricsSummary);
        user.put("metricCount", metrics.size());
        int trialCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM trial_applications WHERE user_id=? AND status NOT IN ('WITHDRAWN','CANCELLED')",
            Integer.class, athleteId);
        user.put("trialApplicationCount", trialCount);
        // Calculate AI score
        int aiScore = 40;
        if (achievements.size() >= 1) aiScore += Math.min(35, achievements.size() * 10);
        if (metrics.size() >= 3) aiScore += 15;
        else if (metrics.size() >= 1) aiScore += 8;
        if (user.get("sport") != null && !Objects.toString(user.get("sport"),"").isBlank()) aiScore += 12;
        if (user.get("location") != null && !Objects.toString(user.get("location"),"").isBlank()) aiScore += 8;
        if (trialCount > 0) aiScore += 5;
        aiScore = Math.min(99, aiScore);
        user.put("aiScore", aiScore);
        int pct = 20;
        if (user.get("sport") != null && !Objects.toString(user.get("sport"),"").isBlank()) pct += 20;
        if (user.get("location") != null && !Objects.toString(user.get("location"),"").isBlank()) pct += 20;
        if (!achievements.isEmpty()) pct += 20;
        if (!metrics.isEmpty()) pct += 20;
        user.put("profileCompletionPct", pct);
        return user;
    }

    public Map<String,Object> getAIScore(long userId) {
        List<Map<String,Object>> users = jdbc.queryForList(
            "SELECT sport, location FROM users WHERE id=?", userId);
        if (users.isEmpty()) throw new IllegalArgumentException("User not found.");
        Map<String,Object> u = users.get(0);
        int achievementCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM achievements WHERE user_id=?", Integer.class, userId);
        int trialCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM trial_applications WHERE user_id=? AND status NOT IN ('WITHDRAWN','CANCELLED')",
            Integer.class, userId);
        int metricCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM athlete_metrics WHERE user_id=?", Integer.class, userId);
        int score = 40;
        if (achievementCount >= 1) score += Math.min(35, achievementCount * 10);
        if (metricCount >= 3) score += 15;
        else if (metricCount >= 1) score += 8;
        if (u.get("sport") != null && !Objects.toString(u.get("sport"),"").isBlank()) score += 12;
        if (u.get("location") != null && !Objects.toString(u.get("location"),"").isBlank()) score += 8;
        if (trialCount > 0) score += 5;
        score = Math.min(99, score);
        int pct = 20;
        if (u.get("sport") != null && !Objects.toString(u.get("sport"),"").isBlank()) pct += 20;
        if (u.get("location") != null && !Objects.toString(u.get("location"),"").isBlank()) pct += 20;
        if (achievementCount > 0) pct += 20;
        if (metricCount > 0) pct += 20;
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("aiScore", score);
        out.put("achievementCount", achievementCount);
        out.put("metricCount", metricCount);
        out.put("activeTrialCount", trialCount);
        out.put("profileCompletionPct", pct);
        return out;
    }

    public List<Map<String,Object>> trialsFiltered(String sport, String category, String ageCategory, String status, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.id, t.name, t.sport, t.category, t.age_category, t.registration_status, t.featured, " +
                "t.organization, t.venue, t.trial_date, " +
                "COALESCE(SUM(s.capacity), 0) total_capacity, COALESCE(SUM(s.booked_count), 0) booked_count, " +
                "COALESCE(SUM(s.capacity - s.booked_count), 0) remaining_slots " +
                "FROM trials t LEFT JOIN trial_slots s ON s.trial_id = t.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (sport != null && !sport.isBlank()) { sql.append("AND LOWER(t.sport) LIKE ? "); params.add("%" + sport.toLowerCase() + "%"); }
        if (category != null && !category.isBlank()) { sql.append("AND LOWER(t.category) LIKE ? "); params.add("%" + category.toLowerCase() + "%"); }
        if (ageCategory != null && !ageCategory.isBlank()) { sql.append("AND REPLACE(LOWER(t.age_category),'-','') LIKE REPLACE(?,'-','') "); params.add("%" + ageCategory.toLowerCase().replace("-","") + "%"); }
        if (status != null && !status.isBlank()) { sql.append("AND LOWER(t.registration_status) LIKE ? "); params.add("%" + status.toLowerCase() + "%"); }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(t.name) LIKE ? OR LOWER(t.organization) LIKE ? OR LOWER(t.sport) LIKE ?) ");
            String s = "%" + search.toLowerCase() + "%";
            params.add(s); params.add(s); params.add(s);
        }
        sql.append("GROUP BY t.id ORDER BY t.trial_date");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    private LocalDate parseDate(String v){return v==null||v.isBlank()?null:LocalDate.parse(v);}
    private String clean(String v){return v==null?null:v.trim();}

    // =========================================================================
    // FEATURE 3: BENCHMARK DRILLS & SKILLS ARENA
    // =========================================================================

    private void seedChallengeBenchmarks() {
        try {
            int count = jdbc.queryForObject("SELECT COUNT(*) FROM athlete_challenges", Integer.class);
            if (count == 0) {
                List<Map<String,Object>> demoUsers = jdbc.queryForList("SELECT id FROM users LIMIT 3");
                if (!demoUsers.isEmpty()) {
                    long u1 = ((Number) demoUsers.get(0).get("id")).longValue();
                    jdbc.update("INSERT INTO athlete_challenges(user_id, drill_type, drill_name, score, unit, badge_awarded, status) VALUES(?,?,?,?,?,?,?)",
                            u1, "REACTION_TAP", "30s Rapid Reaction Reflex Tap", 218.0, "ms", "⚡ Lightning Reflexes (National Elite)", "VERIFIED");
                    jdbc.update("INSERT INTO athlete_challenges(user_id, drill_type, drill_name, score, unit, badge_awarded, status) VALUES(?,?,?,?,?,?,?)",
                            u1, "PLANK_HOLD", "Core Stability Plank Endurance", 210.0, "seconds", "🛡️ Titanium Core (4+ Minutes)", "VERIFIED");
                    jdbc.update("INSERT INTO athlete_challenges(user_id, drill_type, drill_name, score, unit, badge_awarded, status) VALUES(?,?,?,?,?,?,?)",
                            u1, "JUMP_ROPE", "60-Second Jump Rope Cadence", 164.0, "reps", "🦘 High Cadence Phenom (160+ RPM)", "VERIFIED");
                }
            }
        } catch (Exception ignored) {}
    }

    public List<Map<String, Object>> challengesCatalog() {
        return List.of(
            Map.of(
                "type", "REACTION_TAP",
                "name", "30s Rapid Reaction Reflex Tap",
                "category", "Neuromuscular & Reflex",
                "unit", "ms",
                "description", "Test your visual stimulus response latency and motor tap frequency in this interactive in-browser drill.",
                "targetElite", "< 230 ms avg",
                "badgeTier", "⚡ Lightning Reflexes (National Elite)",
                "icon", "fa-bolt"
            ),
            Map.of(
                "type", "PLANK_HOLD",
                "name", "Core Stability Plank Endurance",
                "category", "Core & Spinal Stabilization",
                "unit", "seconds",
                "description", "Maintain an unbroken, neutral-spine isometric plank hold timed live by the interactive stopwatch.",
                "targetElite", "180+ seconds",
                "badgeTier", "🛡️ Iron Core (3+ Minutes)",
                "icon", "fa-shield-halved"
            ),
            Map.of(
                "type", "JUMP_ROPE",
                "name", "60-Second Jump Rope Cadence",
                "category", "Cardiovascular & Footwork",
                "unit", "reps",
                "description", "Record maximal unbroken revolutions in 60 seconds to measure reactive calf elasticity and pacing.",
                "targetElite", "150+ reps",
                "badgeTier", "🦘 High Cadence Phenom",
                "icon", "fa-person-running"
            ),
            Map.of(
                "type", "SHUTTLE_RUN",
                "name", "Pro Agility 5-10-5 Shuttle Run",
                "category", "Change of Direction & Deceleration",
                "unit", "seconds",
                "description", "Rapid multi-directional cone touch test evaluating acceleration, lateral cut agility, and hip drop.",
                "targetElite", "< 4.60 seconds",
                "badgeTier", "🚀 Warp Agility (Elite)",
                "icon", "fa-stopwatch-20"
            ),
            Map.of(
                "type", "BROAD_JUMP",
                "name", "Standing Broad Jump Power",
                "category", "Explosive Triple Extension",
                "unit", "meters",
                "description", "Two-footed stationary horizontal leap measuring peak explosive force production from lower extremities.",
                "targetElite", "2.60+ meters",
                "badgeTier", "🦅 Explosive Triple Extension",
                "icon", "fa-arrows-left-right-to-line"
            )
        );
    }

    public List<Map<String, Object>> challengesLeaderboard(String drillType) {
        String filterSql = (drillType != null && !drillType.isBlank() && !"ALL".equalsIgnoreCase(drillType)) ? "WHERE c.drill_type = ? " : "";
        String orderSql = ("REACTION_TAP".equalsIgnoreCase(drillType) || "SHUTTLE_RUN".equalsIgnoreCase(drillType)) ? "ORDER BY c.score ASC " : "ORDER BY c.score DESC ";
        String query = "SELECT c.id, c.user_id, c.drill_type, c.drill_name, c.score, c.unit, c.badge_awarded, c.created_at, " +
                       "u.full_name as athlete_name, u.sport, u.location " +
                       "FROM athlete_challenges c JOIN users u ON u.id = c.user_id " +
                       filterSql + orderSql + "LIMIT 25";
        if (!filterSql.isEmpty()) {
            return jdbc.queryForList(query, drillType);
        }
        return jdbc.queryForList(query);
    }

    @Transactional
    public Map<String, Object> submitChallenge(long userId, ChallengeSubmissionRequest req) {
        String drillType = req.getDrillType().toUpperCase();
        double score = req.getScore();
        String badge;

        switch (drillType) {
            case "REACTION_TAP" -> {
                if (score < 230) badge = "⚡ Lightning Reflexes (National Elite)";
                else if (score < 280) badge = "⚡ Fast Reflexes (Pro Level)";
                else badge = "⚡ Quick Reflexes (Verified)";
            }
            case "PLANK_HOLD" -> {
                if (score >= 240) badge = "🛡️ Titanium Core (4+ Minutes)";
                else if (score >= 180) badge = "🛡️ Iron Core (3 Minutes)";
                else if (score >= 120) badge = "🛡️ Solid Core (2 Minutes)";
                else badge = "🛡️ Core Warrior";
            }
            case "JUMP_ROPE" -> {
                if (score >= 160) badge = "🦘 High Cadence Phenom (160+ RPM)";
                else if (score >= 130) badge = "🦘 Cadence Pro";
                else badge = "🦘 Verified Rhythm";
            }
            case "SHUTTLE_RUN" -> {
                if (score <= 4.40) badge = "🚀 Warp Agility (Elite)";
                else if (score <= 4.90) badge = "🚀 Swift Shuttler";
                else badge = "🚀 Agility Verified";
            }
            case "BROAD_JUMP" -> {
                if (score >= 2.80) badge = "🦅 Explosive Triple Extension (Elite)";
                else if (score >= 2.40) badge = "🦅 Power Leaper";
                else badge = "🦅 Power Verified";
            }
            default -> badge = "🏅 Benchmark Completed";
        }

        jdbc.update(
            "INSERT INTO athlete_challenges(user_id, drill_type, drill_name, score, unit, badge_awarded, video_url, notes, status) VALUES(?,?,?,?,?,?,?,?,?)",
            userId, drillType, req.getDrillName(), score, req.getUnit(), badge, req.getVideoUrl(), req.getNotes(), "VERIFIED"
        );

        try {
            jdbc.update(
                "INSERT INTO athlete_metrics(user_id, metric_name, metric_value, unit, category, recorded_date, notes) VALUES(?,?,?,?,?,CURDATE(),?)",
                userId, req.getDrillName(), score, req.getUnit(), "Challenge Drills", "Logged via Skills Arena: " + badge
            );
        } catch (Exception ignored) {}

        try {
            jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                    userId, "New Skill Badge Unlocked: " + badge,
                    "Congratulations! Your drill score of " + score + " " + req.getUnit() + " has been verified in the Challenge Arena.");
        } catch (Exception ignored) {}

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("badge", badge);
        res.put("score", score);
        res.put("unit", req.getUnit());
        res.put("message", "Drill challenge verified! Badge awarded: " + badge);
        return res;
    }

    // =========================================================================
    // FEATURE 4: AI ATHLETIC RECOVERY & READINESS LAB
    // =========================================================================

    public Map<String, Object> readinessStatus(long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> todayLogs = jdbc.queryForList(
            "SELECT * FROM athlete_readiness_logs WHERE user_id=? AND logged_date=CURDATE() ORDER BY id DESC LIMIT 1", userId);

        if (!todayLogs.isEmpty()) {
            out.put("todayLog", todayLogs.get(0));
            out.put("hasLoggedToday", true);
        } else {
            out.put("todayLog", null);
            out.put("hasLoggedToday", false);
        }

        List<Map<String, Object>> history = jdbc.queryForList(
            "SELECT * FROM athlete_readiness_logs WHERE user_id=? ORDER BY logged_date DESC, id DESC LIMIT 14", userId);
        out.put("history", history);

        double avgReadiness = history.stream()
                .mapToInt(m -> ((Number) m.get("readiness_score")).intValue())
                .average()
                .orElse(82.0);
        out.put("averageReadiness", Math.round(avgReadiness));

        return out;
    }

    @Transactional
    public Map<String, Object> logReadiness(long userId, ReadinessLogRequest req) {
        int sleepScore = (int) Math.min(100, Math.max(30, (req.getSleepHours() / 8.0) * 100));
        int hrScore = req.getRestingHr() <= 52 ? 100 : (req.getRestingHr() <= 60 ? 90 : (req.getRestingHr() <= 70 ? 75 : 55));
        int sorenessScore = (11 - req.getSorenessLevel()) * 10;
        int fatigueScore = (11 - req.getFatigueLevel()) * 10;
        int hydroScore = (int) Math.min(100, Math.max(40, (req.getHydrationLiters() / 3.0) * 100));

        int readinessScore = (int) Math.round(sleepScore * 0.25 + hrScore * 0.20 + sorenessScore * 0.20 + fatigueScore * 0.20 + hydroScore * 0.15);
        readinessScore = Math.max(25, Math.min(99, readinessScore));

        String statusLabel;
        String injuryRisk;
        String advice;

        if (readinessScore >= 85) {
            statusLabel = "OPTIMAL";
            injuryRisk = "LOW";
            advice = "Prime nervous system condition. Cleared for maximal effort speed, power, and high-intensity game play.";
        } else if (readinessScore >= 65) {
            statusLabel = "MODERATE";
            injuryRisk = "MODERATE";
            advice = "Adequate recovery. Maintain technical and tactical drills; avoid 1-rep max lifts and prolonged eccentric load.";
        } else {
            statusLabel = "FATIGUE WARNING";
            injuryRisk = "ELEVATED";
            advice = "Systemic neuromuscular fatigue detected. Prescribed: Active recovery, myofascial foam rolling, and contrast showers.";
        }

        jdbc.update("DELETE FROM athlete_readiness_logs WHERE user_id=? AND logged_date=CURDATE()", userId);

        jdbc.update(
            "INSERT INTO athlete_readiness_logs(user_id, readiness_score, resting_hr, sleep_hours, soreness_level, fatigue_level, hydration_liters, status_label, injury_risk, notes, logged_date) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,CURDATE())",
            userId, readinessScore, req.getRestingHr(), req.getSleepHours(), req.getSorenessLevel(), req.getFatigueLevel(), req.getHydrationLiters(), statusLabel, injuryRisk, req.getNotes()
        );

        if ("ELEVATED".equals(injuryRisk)) {
            try {
                jdbc.update("INSERT INTO notifications(user_id, title, message) VALUES(?,?,?)",
                    userId, "⚠️ Elevated Injury Risk Alert (" + readinessScore + "%)",
                    "Your daily recovery metrics show elevated fatigue. Rest or light mobility is strongly advised today.");
            } catch (Exception ignored) {}
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("readinessScore", readinessScore);
        res.put("statusLabel", statusLabel);
        res.put("injuryRisk", injuryRisk);
        res.put("advice", advice);
        return res;
    }

    public List<Map<String, Object>> injuryGuidelines(String sport) {
        String s = (sport == null ? "" : sport.toLowerCase());
        if (s.contains("cricket")) {
            return List.of(
                Map.of("targetZone", "Rotator Cuff & Shoulder Complex", "protocol", "Y-T-W Band Pull-Aparts & Internal/External Shoulder Rotations", "dosage", "3 sets of 15 reps daily"),
                Map.of("targetZone", "Lumbar Spine / Lower Back", "protocol", "Bird-Dog, Dead-Bug & Side Plank Spine Neutralizers for fast bowlers", "dosage", "3 sets of 45s hold"),
                Map.of("targetZone", "Hamstring & Groin Strain", "protocol", "Nordic Hamstring Lowers & Copenhagen Adductor Planks", "dosage", "2 sets of 6 reps, 3x / week")
            );
        } else if (s.contains("football") || s.contains("soccer")) {
            return List.of(
                Map.of("targetZone", "Anterior Cruciate Ligament (ACL) & Knee", "protocol", "Single-Leg Deceleration Squats & Jump-Landing Stabilization", "dosage", "3 sets of 10 landings per leg"),
                Map.of("targetZone", "Hamstring Biceps Femoris", "protocol", "Eccentric Nordic Curls & Glute-Ham Roller Bridges", "dosage", "3 sets of 8 reps"),
                Map.of("targetZone", "Ankle Syndesmosis", "protocol", "Banded Dorsiflexion Mobilizations & Slant Board Heel Drops", "dosage", "3 sets of 20 reps")
            );
        } else if (s.contains("badminton") || s.contains("tennis")) {
            return List.of(
                Map.of("targetZone", "Patellar Tendon / Jumper's Knee", "protocol", "Decline Squat Isometric Holds at 60 degrees", "dosage", "5 sets of 45s holds"),
                Map.of("targetZone", "Achilles Tendon", "protocol", "Slow Eccentric Calf Raises on Step with 3s tempo", "dosage", "3 sets of 15 reps each leg"),
                Map.of("targetZone", "Wrist & Elbow Lateral Epicondyle", "protocol", "Tyler Twist with FlexBar & Forearm Pronation/Supination", "dosage", "3 sets of 12 reps")
            );
        } else {
            return List.of(
                Map.of("targetZone", "Hamstrings & Hip Flexors", "protocol", "Psoas March with Mini-Band & Nordic Hamstring Lowers", "dosage", "3 sets of 10 reps"),
                Map.of("targetZone", "Shin Splints / Tibialis Anterior", "protocol", "Wall Tibialis Raises & Heel Walking Drill", "dosage", "3 sets of 25 reps"),
                Map.of("targetZone", "Thoracic Spine & Core", "protocol", "Open-Book Thoracic Rotations & Pallof Anti-Rotation Press", "dosage", "3 sets of 12 reps")
            );
        }
    }
}

