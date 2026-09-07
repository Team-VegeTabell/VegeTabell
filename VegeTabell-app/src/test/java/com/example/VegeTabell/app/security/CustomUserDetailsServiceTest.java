package com.example.VegeTabell.app.security;

import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_buyer_returnsRoleBuyerAuthority() {
        User user = new User();
        user.setEmail("buyer@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.BUYER);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("buyer@example.com");

        assertTrue(details instanceof CustomUserDetails);
        assertEquals("buyer@example.com", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BUYER")));
    }

    @Test
    void loadUserByUsername_seller_returnsRoleSellerAuthority() {
        User user = new User();
        user.setEmail("seller@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.SELLER);
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("seller@example.com");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER")));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@example.com"));
    }
}
