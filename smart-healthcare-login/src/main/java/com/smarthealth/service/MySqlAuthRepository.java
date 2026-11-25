package com.smarthealth.service;

import com.smarthealth.model.JoinSession;
import com.smarthealth.model.Role;
import com.smarthealth.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * JDBC AuthRepository targeting XAMPP MySQL / MariaDB (localhost:3306/smart_healthcare).
 * Override connection settings via SMARTHEALTH_DB_URL, SMARTHEALTH_DB_USER, and SMARTHEALTH_DB_PASSWORD.
 */
public class MySqlAuthRepository implements AuthRepository {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/smart_healthcare"
                    + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;
    private volatile boolean schemaInitialized;

    public MySqlAuthRepository() {
        this(
                resolveEnv("SMARTHEALTH_DB_URL", DEFAULT_URL),
                resolveEnv("SMARTHEALTH_DB_USER", DEFAULT_USER),
                resolveNullableEnv("SMARTHEALTH_DB_PASSWORD", DEFAULT_PASSWORD)
        );
    }

    public MySqlAuthRepository(String jdbcUrl, String dbUser, String dbPassword) {
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        loadDriver();
    }

    @Override
    public Optional<User> findUserByUsername(String normalizedUsername) {
        ensureSchema();
        String sql = "SELECT id, username, full_name, role, salt, password_hash "
                + "FROM users WHERE LOWER(username) = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Role role = Role.fromString(rs.getString("role"));
                return Optional.of(new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        role != null ? role : Role.PATIENT,
                        rs.getString("salt"),
                        rs.getString("password_hash")
                ));
            }
        } catch (SQLException ex) {
            throw databaseError("Failed to look up user in XAMPP MySQL", ex);
        }
    }

    @Override
    public void saveUser(User user) {
        ensureSchema();
        String sql = "INSERT INTO users (id, username, full_name, role, salt, password_hash) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), role = VALUES(role), "
                + "salt = VALUES(salt), password_hash = VALUES(password_hash)";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername().trim().toLowerCase());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getSalt());
            ps.setString(6, user.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw databaseError("Failed to save user to XAMPP MySQL", ex);
        }
    }

    @Override
    public Optional<JoinSession> findJoinSessionByCode(String normalizedCode) {
        ensureSchema();
        String sql = "SELECT code, title, department, doctor_name, expires_at, active "
                + "FROM join_codes WHERE code = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Timestamp ts = rs.getTimestamp("expires_at");
                Instant expiresAt = ts != null ? ts.toInstant() : null;
                return Optional.of(new JoinSession(
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("department"),
                        rs.getString("doctor_name"),
                        expiresAt,
                        rs.getBoolean("active")
                ));
            }
        } catch (SQLException ex) {
            throw databaseError("Failed to look up join code in XAMPP MySQL", ex);
        }
    }

    @Override
    public void saveJoinSession(JoinSession session) {
        ensureSchema();
        String sql = "INSERT INTO join_codes (code, title, department, doctor_name, expires_at, active) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE title = VALUES(title), department = VALUES(department), "
                + "doctor_name = VALUES(doctor_name), expires_at = VALUES(expires_at), active = VALUES(active)";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.getCode());
            ps.setString(2, session.getTitle());
            ps.setString(3, session.getDepartment());
            ps.setString(4, session.getDoctorName());
            ps.setTimestamp(5, session.getExpiresAt() != null ? Timestamp.from(session.getExpiresAt()) : null);
            ps.setBoolean(6, session.isActive());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw databaseError("Failed to save join code to XAMPP MySQL", ex);
        }
    }

    public boolean isEmpty() {
        ensureSchema();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException ex) {
            throw databaseError("Failed to query users count in XAMPP MySQL", ex);
        }
    }

    public boolean hasJoinCodes() {
        ensureSchema();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM join_codes")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            throw databaseError("Failed to query join_codes count in XAMPP MySQL", ex);
        }
    }

    private void ensureSchema() {
        if (schemaInitialized) {
            return;
        }
        synchronized (this) {
            if (schemaInitialized) {
                return;
            }
            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS users ("
                                + "id VARCHAR(32) PRIMARY KEY,"
                                + "username VARCHAR(120) NOT NULL UNIQUE,"
                                + "full_name VARCHAR(120) NOT NULL,"
                                + "role VARCHAR(20) NOT NULL,"
                                + "salt VARCHAR(64) NOT NULL,"
                                + "password_hash VARCHAR(64) NOT NULL,"
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS join_codes ("
                                + "code VARCHAR(16) PRIMARY KEY,"
                                + "title VARCHAR(160) NOT NULL,"
                                + "department VARCHAR(100) NOT NULL,"
                                + "doctor_name VARCHAR(120) NOT NULL,"
                                + "expires_at TIMESTAMP NULL,"
                                + "active TINYINT(1) NOT NULL DEFAULT 1,"
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
                schemaInitialized = true;
            } catch (SQLException ex) {
                throw databaseError("Could not connect to XAMPP MySQL", ex);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL JDBC driver not found on classpath", ex);
        }
    }

    private static IllegalStateException databaseError(String prefix, SQLException ex) {
        return new IllegalStateException(
                prefix + " (check that XAMPP MySQL is running on localhost:3306): " + ex.getMessage(),
                ex
        );
    }

    private static String resolveEnv(String name, String fallback) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val.trim() : fallback;
    }

    private static String resolveNullableEnv(String name, String fallback) {
        String val = System.getenv(name);
        return val != null ? val : fallback;
    }
}
