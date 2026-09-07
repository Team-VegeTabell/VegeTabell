package com.example.VegeTabell.app.security;

import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoleMatchLoginIntegrationTest {

    private static final String RAW_PASSWORD = "password123";
    private static final String EMAIL = "buyer@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private User buyerUser() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(UserRole.BUYER);
        user.setDisplayName("テスト買い手");
        return user;
    }

    @Test
    void loginWithMismatchedRole_isRejected() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyerUser()));

        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("role", "seller")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?error=role")));
    }

    @Test
    void loginWithMatchingRole_succeeds() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyerUser()));

        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("role", "buyer")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", not(containsString("/login"))));
    }
}
