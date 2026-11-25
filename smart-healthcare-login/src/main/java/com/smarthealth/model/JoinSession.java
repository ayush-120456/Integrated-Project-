package com.smarthealth.model;

import java.time.Instant;

/**
 * Represents a healthcare appointment, telehealth room, or clinic portal invite code.
 */
public class JoinSession {
    private final String code;
    private final String title;
    private final String department;
    private final String doctorName;
    private final Instant expiresAt;
    private final boolean active;

    public JoinSession(String code, String title, String department, String doctorName, Instant expiresAt, boolean active) {
        this.code = code;
        this.title = title;
        this.department = department;
        this.doctorName = doctorName;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDepartment() {
        return department;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
