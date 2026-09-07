package com.example.VegeTabell.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRoleMatcherTest {

    private final LoginRoleMatcher matcher = new LoginRoleMatcher();

    private Authentication authenticationWithAuthority(String authority) {
        return new TestingAuthenticationToken("user", "password", List.of(new SimpleGrantedAuthority(authority)));
    }

    @Test
    void matches_whenSelectedRoleMatchesAuthority_buyer() {
        Authentication authentication = authenticationWithAuthority("ROLE_BUYER");
        assertTrue(matcher.matches(authentication, "buyer"));
    }

    @Test
    void matches_whenSelectedRoleMatchesAuthority_seller() {
        Authentication authentication = authenticationWithAuthority("ROLE_SELLER");
        assertTrue(matcher.matches(authentication, "seller"));
    }

    @Test
    void matches_whenSelectedRoleDoesNotMatchAuthority() {
        Authentication authentication = authenticationWithAuthority("ROLE_BUYER");
        assertFalse(matcher.matches(authentication, "seller"));
    }

    @Test
    void matches_whenSelectedRoleIsNull() {
        Authentication authentication = authenticationWithAuthority("ROLE_BUYER");
        assertFalse(matcher.matches(authentication, null));
    }

    @Test
    void matches_whenSelectedRoleIsBlank() {
        Authentication authentication = authenticationWithAuthority("ROLE_BUYER");
        assertFalse(matcher.matches(authentication, "  "));
    }

    @Test
    void matches_whenSelectedRoleIsInvalid() {
        Authentication authentication = authenticationWithAuthority("ROLE_BUYER");
        assertFalse(matcher.matches(authentication, "admin"));
    }
}
