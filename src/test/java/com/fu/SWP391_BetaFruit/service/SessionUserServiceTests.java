package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionUserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final SessionUserService service = new SessionUserService(users);

    @Test void resolvesIntegerFromLoginSessionInsteadOfLegacyKey() {
        var session = new MockHttpSession();
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        session.setAttribute("CURRENT_USER_ID", 99);
        var user = new User(); user.setUserId(7);
        when(users.findById(7)).thenReturn(Optional.of(user));
        assertSame(user, service.findCurrentUser(session).orElseThrow());
        verify(users).findById(7);
        verifyNoMoreInteractions(users);
    }

    @Test void missingOrInvalidSessionIdentityDoesNotQueryUsers() {
        assertTrue(service.findCurrentUser(null).isEmpty());
        var session = new MockHttpSession();
        assertTrue(service.findCurrentUser(session).isEmpty());
        session.setAttribute("CURRENT_USER_ID", 7);
        assertTrue(service.findCurrentUser(session).isEmpty());
        for (Object invalid : List.of("7", "invalid", 7L, 7.5, true)) {
            session.setAttribute("LOGGED_IN_USER_ID", invalid);
            assertTrue(service.findCurrentUser(session).isEmpty());
        }
        verifyNoInteractions(users);
    }

    @Test void deletedUserIsNotAuthenticated() {
        var session = new MockHttpSession(); session.setAttribute("LOGGED_IN_USER_ID", 7);
        when(users.findById(7)).thenReturn(Optional.empty());
        assertTrue(service.findCurrentUser(session).isEmpty());
    }
}
