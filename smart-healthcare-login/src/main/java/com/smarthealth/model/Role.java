package com.smarthealth.model;

/**
 * Supported user roles in the Smart Healthcare System.
 */
public enum Role {
    PATIENT("Patient"),
    DOCTOR("Doctor"),
    STAFF("Staff / Admin");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a role string case-insensitively, returning null if invalid.
     */
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
