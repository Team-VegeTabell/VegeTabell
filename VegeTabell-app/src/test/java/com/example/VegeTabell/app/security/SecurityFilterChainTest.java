package com.example.VegeTabell.app.security;

import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRootRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void unauthenticatedSellerPathRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void loginPageIsNotBlockedByAuthentication() throws Exception {
        // Step4でAuthController#loginを実装したため、200が正しい期待値になった。
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUser_postLogout_redirectsToLoginWithLogoutParam() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.BUYER);
        user.setDisplayName("テスト買い手");

        mockMvc.perform(post("/logout")
                        .with(user(new CustomUserDetails(user)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?logout"));
    }

    @Test
    void postLogout_withoutCsrf_isForbidden() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.BUYER);
        user.setDisplayName("テスト買い手");

        mockMvc.perform(post("/logout")
                        .with(user(new CustomUserDetails(user))))
                .andExpect(status().isForbidden());
    }
}
