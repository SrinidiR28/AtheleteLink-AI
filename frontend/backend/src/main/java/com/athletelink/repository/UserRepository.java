package com.athletelink.repository;

import com.athletelink.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Plain Spring JDBC (JdbcTemplate) repository - no JPA/Hibernate.
 * Talks directly to the `users` table defined in database/schema.sql.
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        String role = "ATHLETE";
        String status = "ACTIVE";
        try { role = rs.getString("role"); } catch (Exception ignored) {}
        try { status = rs.getString("status"); } catch (Exception ignored) {}
        if (role == null || role.isBlank()) role = "ATHLETE";
        if (status == null || status.isBlank()) status = "ACTIVE";

        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("sport"),
                rs.getString("location"),
                role,
                status,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
        );
    };

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, full_name, email, password_hash, sport, location, role, status, created_at " +
                "FROM users WHERE email = ?";
        List<User> results = jdbcTemplate.query(sql, USER_ROW_MAPPER, email);
        return results.stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, full_name, email, password_hash, sport, location, role, status, created_at " +
                "FROM users WHERE id = ?";
        List<User> results = jdbcTemplate.query(sql, USER_ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public List<User> findAll() {
        String sql = "SELECT id, full_name, email, password_hash, sport, location, role, status, created_at " +
                "FROM users ORDER BY id DESC";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER);
    }

    public User save(User user) {
        String role = (user.getRole() == null || user.getRole().isBlank()) ? "ATHLETE" : user.getRole();
        String status = (user.getStatus() == null || user.getStatus().isBlank()) ? "ACTIVE" : user.getStatus();

        String sql = "INSERT INTO users (full_name, email, password_hash, sport, location, role, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getSport());
            ps.setString(5, user.getLocation());
            ps.setString(6, role);
            ps.setString(7, status);
            ps.setTimestamp(8, Timestamp.valueOf(user.getCreatedAt()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        return user;
    }

    public int updateProfile(Long id, String fullName, String sport, String location) {
        String sql = "UPDATE users SET full_name = ?, sport = ?, location = ? WHERE id = ?";
        return jdbcTemplate.update(sql, fullName, sport, location, id);
    }

    public int updateRole(Long id, String role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        return jdbcTemplate.update(sql, role, id);
    }

    public int updateStatus(Long id, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        return jdbcTemplate.update(sql, status, id);
    }

    public int updateUser(Long id, String fullName, String email, String sport, String location, String role, String status) {
        String sql = "UPDATE users SET full_name = ?, email = ?, sport = ?, location = ?, role = ?, status = ? WHERE id = ?";
        return jdbcTemplate.update(sql, fullName, email, sport, location, role, status, id);
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
