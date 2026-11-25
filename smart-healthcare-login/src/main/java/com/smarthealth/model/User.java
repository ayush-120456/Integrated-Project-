package com.smarthealth.model;

/**
 * Represents an authenticated user account in the Smart Healthcare System.
 */
public class User {
    private final String id;
    private final String username;
    private final String fullName;
    private final Role role;
    private final String salt;
    private final String passwordHash;

    public User(String id, String username, String fullName, Role role, String salt, String passwordHash) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public String getSalt() {
        return salt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
