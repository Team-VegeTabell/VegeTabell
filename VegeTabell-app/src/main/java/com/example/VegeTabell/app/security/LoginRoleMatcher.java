package com.example.VegeTabell.app.security;

import com.example.VegeTabell.app.entity.type.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class LoginRoleMatcher {

    public boolean matches(Authentication authentication, String selectedRole) {
        if (selectedRole == null || selectedRole.isBlank()) {
            return false;
        }
        UserRole requested;
        try {
            requested = UserRole.fromValue(selectedRole.toLowerCase());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String expectedAuthority = "ROLE_" + requested.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(expectedAuthority));
    }
}
