package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.controller.AuthController;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.entity.converter.RoleNameConverter;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.interceptor.ShipperInterceptor;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DeliveryStaffLoginTests {
    @Test
    void canonicalEnumIsStoredReadIntoSessionAndAuthorizedForShipper() throws Exception {
        var config = new Configuration().addAnnotatedClass(User.class).addAnnotatedClass(Role.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:delivery_staff_login;MODE=MSSQLServer")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = config.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            Role role = new Role();
            role.setRoleName(RoleName.DELIVERY_STAFF);
            em.persist(role);
            User user = new User();
            user.setUsername("driver");
            user.setEmail("driver@example.test");
            user.setFullName("Driver");
            user.setPasswordHash(BCrypt.hashpw("test-password", BCrypt.gensalt(4)));
            user.setStatus(UserStatus.ACTIVE);
            user.getRoles().add(role);
            em.persist(user);
            em.flush();
            em.clear();
            assertEquals("DELIVERY_STAFF", em.createNativeQuery(
                    "SELECT roleName FROM [Role] WHERE RoleId = :id", String.class)
                    .setParameter("id", role.getRoleId()).getSingleResult());

            var auth = new AuthServiceImpl();
            ReflectionTestUtils.setField(auth, "userRepository",
                    new JpaRepositoryFactory(em).getRepository(UserRepository.class));
            var controller = new AuthController();
            ReflectionTestUtils.setField(controller, "authService", auth);
            var session = new MockHttpSession();
            standaloneSetup(controller).build().perform(post("/auth/login").session(session)
                            .param("email", "driver@example.test").param("password", "test-password"))
                    .andExpect(redirectedUrl("/shipper/orders"))
                    .andExpect(request().sessionAttribute("LOGGED_IN_USER_ID", user.getUserId()))
                    .andExpect(request().sessionAttribute("LOGGED_IN_ROLES", List.of(RoleName.DELIVERY_STAFF)));

            var request = new MockHttpServletRequest("GET", "/shipper/orders");
            request.setSession(session);
            assertTrue(new ShipperInterceptor().preHandle(request, new MockHttpServletResponse(), new Object()));
            em.getTransaction().rollback();
        }
    }

    @Test
    void deliveryRoleMustMatchTeamEnumWithoutAliases() {
        var converter = new RoleNameConverter();
        for (RoleName role : RoleName.values()) {
            assertEquals(role, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(role)));
        }
        for (String alias : List.of("SHIPPER", "shipper", "DELIVERER", " DELIVERY_STAFF ")) {
            assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(alias));
        }
    }
}
