package com.smarthealth.service;

import com.smarthealth.model.JoinSession;
import com.smarthealth.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory AuthRepository used for unit testing without an external database.
 */
public class InMemoryAuthRepository implements AuthRepository {
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, JoinSession> joinSessionsByCode = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findUserByUsername(String normalizedUsername) {
        return Optional.ofNullable(usersByUsername.get(normalizedUsername));
    }

    @Override
    public void saveUser(User user) {
        usersByUsername.put(user.getUsername().trim().toLowerCase(), user);
    }

    @Override
    public Optional<JoinSession> findJoinSessionByCode(String normalizedCode) {
        return Optional.ofNullable(joinSessionsByCode.get(normalizedCode));
    }

    @Override
    public void saveJoinSession(JoinSession session) {
        joinSessionsByCode.put(session.getCode(), session);
    }
}
