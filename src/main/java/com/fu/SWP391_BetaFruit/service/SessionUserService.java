package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Resolves the authenticated user that the login flow stores in the HTTP session. */
@Service
public class SessionUserService {
    public static final String CURRENT_USER_ID = "LOGGED_IN_USER_ID";

    private final UserRepository userRepository;

    public SessionUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findCurrentUser(HttpSession session) {
        if (session == null) return Optional.empty();
        Object rawUserId = session.getAttribute(CURRENT_USER_ID);
        if (rawUserId instanceof Integer userId) {
            return userRepository.findById(userId);
        }
        return Optional.empty();
    }
}
