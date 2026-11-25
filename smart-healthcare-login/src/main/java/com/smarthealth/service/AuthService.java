package com.smarthealth.service;

import com.smarthealth.model.JoinSession;
import com.smarthealth.model.Role;
import com.smarthealth.model.User;
import com.smarthealth.util.PasswordUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles credential authentication and "Join with Code" session validation
 * backed by XAMPP MySQL (or an in-memory repository for tests).
 */
public class AuthService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4}-?[A-Z0-9]{4}$");

    private final AuthRepository repository;
    private volatile boolean demoSeedChecked;

    public AuthService() {
        this(new MySqlAuthRepository(), false);
    }

    public AuthService(AuthRepository repository, boolean seedImmediately) {
        this.repository = repository;
        if (seedImmediately) {
            seedDemoData();
            this.demoSeedChecked = true;
        }
    }

    public static AuthService inMemoryForTests() {
        return new AuthService(new InMemoryAuthRepository(), true);
    }

    /**
     * Authenticates a user by username/email, plaintext password, and optional expected role.
     */
    public AuthResult authenticate(String username, String rawPassword, Role expectedRole) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isEmpty()) {
            return AuthResult.failure("Please enter both your username and password.");
        }

        try {
            ensureDemoSeeded();
            Optional<User> found = repository.findUserByUsername(username.trim().toLowerCase());
            if (found.isEmpty()) {
                return AuthResult.failure("Invalid username or password.");
            }

            User user = found.get();
            if (!PasswordUtil.verifyPassword(rawPassword, user.getSalt(), user.getPasswordHash())) {
                return AuthResult.failure("Invalid username or password.");
            }

            if (expectedRole != null && user.getRole() != expectedRole) {
                return AuthResult.failure("This account is not registered under the " + expectedRole.getDisplayName() + " role.");
            }

            return AuthResult.success(user);
        } catch (RuntimeException ex) {
            return AuthResult.failure(ex.getMessage());
        }
    }

    /**
     * Validates an appointment / clinic invite code and returns the matching active session.
     * Accepts codes with or without a hyphen (e.g., "CARE-4821" or "care4821").
     */
    public JoinResult validateJoinCode(String rawCode, String participantName) {
        if (rawCode == null || rawCode.isBlank()) {
            return JoinResult.failure("Please enter an access code to join.");
        }

        String normalized = normalizeCode(rawCode);
        if (normalized == null) {
            return JoinResult.failure("Access code must be 8 alphanumeric characters (for example, CARE-4821).");
        }

        try {
            ensureDemoSeeded();
            Optional<JoinSession> found = repository.findJoinSessionByCode(normalized);
            if (found.isEmpty()) {
                return JoinResult.failure("No healthcare session found for code " + normalized + ".");
            }

            JoinSession session = found.get();
            if (!session.isActive()) {
                return JoinResult.failure("Access code " + normalized + " has been deactivated.");
            }
            if (session.isExpired()) {
                return JoinResult.failure("Access code " + normalized + " has expired. Please request a new code.");
            }

            String resolvedName = (participantName == null || participantName.isBlank())
                    ? "Guest Patient"
                    : participantName.trim();
            return JoinResult.success(session, resolvedName);
        } catch (RuntimeException ex) {
            return JoinResult.failure(ex.getMessage());
        }
    }

    /**
     * Registers or updates a user account in the configured repository.
     */
    public void registerUser(String id, String username, String fullName, Role role, String rawPassword) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);
        repository.saveUser(new User(id, username.trim().toLowerCase(), fullName, role, salt, hash));
    }

    /**
     * Registers or updates a join code in the configured repository.
     */
    public void registerJoinSession(JoinSession session) {
        String normalized = normalizeCode(session.getCode());
        if (normalized == null) {
            throw new IllegalArgumentException("Invalid join code format: " + session.getCode());
        }
        repository.saveJoinSession(new JoinSession(
                normalized,
                session.getTitle(),
                session.getDepartment(),
                session.getDoctorName(),
                session.getExpiresAt(),
                session.isActive()
        ));
    }

    /**
     * Normalizes an input code into XXXX-XXXX uppercase format, or returns null if malformed.
     */
    public static String normalizeCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String compact = rawCode.trim().toUpperCase().replaceAll("\\s+", "");
        if (!CODE_PATTERN.matcher(compact).matches()) {
            return null;
        }
        String alnum = compact.replace("-", "");
        return alnum.substring(0, 4) + "-" + alnum.substring(4, 8);
    }

    private void ensureDemoSeeded() {
        if (demoSeedChecked) {
            return;
        }
        synchronized (this) {
            if (demoSeedChecked) {
                return;
            }
            if (repository instanceof MySqlAuthRepository mysqlRepo) {
                if (mysqlRepo.isEmpty()) {
                    seedDemoUsers();
                }
                if (!mysqlRepo.hasJoinCodes()) {
                    seedDemoJoinCodes();
                }
            } else {
                seedDemoData();
            }
            demoSeedChecked = true;
        }
    }

    private void seedDemoData() {
        seedDemoUsers();
        seedDemoJoinCodes();
    }

    private void seedDemoUsers() {
        registerUser("P-1001", "patient@smarthealth.org", "Maya Patel", Role.PATIENT, "Patient123!");
        registerUser("D-2001", "doctor@smarthealth.org", "Dr. Arjun Rao", Role.DOCTOR, "Doctor123!");
        registerUser("S-3001", "staff@smarthealth.org", "Elena Brooks", Role.STAFF, "Staff123!");
    }

    private void seedDemoJoinCodes() {
        Instant tomorrow = Instant.now().plus(30, ChronoUnit.DAYS);
        Instant yesterday = Instant.now().minus(24, ChronoUnit.HOURS);

        registerJoinSession(new JoinSession(
                "CARE-4821",
                "Cardiology Follow-Up Consultation",
                "Cardiology",
                "Dr. Arjun Rao",
                tomorrow,
                true
        ));
        registerJoinSession(new JoinSession(
                "HLTH-9034",
                "General Telehealth Check-In",
                "Primary Care",
                "Dr. Priya Nair",
                tomorrow,
                true
        ));
        registerJoinSession(new JoinSession(
                "OLDX-1100",
                "Expired Lab Review",
                "Diagnostics",
                "Dr. Samira Khan",
                yesterday,
                true
        ));
    }

    public static final class AuthResult {
        private final boolean success;
        private final User user;
        private final String errorMessage;

        private AuthResult(boolean success, User user, String errorMessage) {
            this.success = success;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, user, null);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public Optional<User> getUser() {
            return Optional.ofNullable(user);
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static final class JoinResult {
        private final boolean success;
        private final JoinSession session;
        private final String participantName;
        private final String errorMessage;

        private JoinResult(boolean success, JoinSession session, String participantName, String errorMessage) {
            this.success = success;
            this.session = session;
            this.participantName = participantName;
            this.errorMessage = errorMessage;
        }

        public static JoinResult success(JoinSession session, String participantName) {
            return new JoinResult(true, session, participantName, null);
        }

        public static JoinResult failure(String errorMessage) {
            return new JoinResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public Optional<JoinSession> getSession() {
            return Optional.ofNullable(session);
        }

        public String getParticipantName() {
            return participantName;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
