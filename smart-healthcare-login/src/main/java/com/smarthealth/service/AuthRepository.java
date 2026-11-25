package com.smarthealth.service;

import com.smarthealth.model.JoinSession;
import com.smarthealth.model.User;

import java.util.Optional;

/**
 * Persistence abstraction for user accounts and join-code sessions.
 */
public interface AuthRepository {
    Optional<User> findUserByUsername(String normalizedUsername);

    void saveUser(User user);

    Optional<JoinSession> findJoinSessionByCode(String normalizedCode);

    void saveJoinSession(JoinSession session);
}
