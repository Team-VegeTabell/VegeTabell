package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.ShopRepository;
import com.example.VegeTabell.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ShopRepository shopRepository;

    @Test
    void getLogin_isReachableWithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void getSignup_showsFormWithBuyerDefault() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeExists("signupForm"));
    }

    @Test
    void postSignup_buyerHappyPath_redirectsAndSavesUserOnly() throws Exception {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/signup")
                        .param("role", "BUYER")
                        .param("email", "buyer@example.com")
                        .param("password", "password123")
                        .param("displayName", "テスト買い手")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?registered"));

        verify(userRepository).save(any(User.class));
        verify(shopRepository, never()).save(any());
    }

    @Test
    void postSignup_sellerHappyPath_redirectsAndSavesUserAndShop() throws Exception {
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/signup")
                        .param("role", "SELLER")
                        .param("email", "seller@example.com")
                        .param("password", "password123")
                        .param("shopName", "大地の恵み 八百屋")
                        .param("address", "東京都世田谷区1-1-1")
                        .param("pickupNote", "世田谷駅北口から徒歩3分")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?registered"));

        verify(userRepository).save(any(User.class));
        verify(shopRepository).save(any());
    }

    @Test
    void postSignup_sellerMissingShopName_reRendersWithFieldError() throws Exception {
        when(userRepository.findByEmail("seller2@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/signup")
                        .param("role", "SELLER")
                        .param("email", "seller2@example.com")
                        .param("password", "password123")
                        .param("address", "東京都世田谷区1-1-1")
                        .param("pickupNote", "世田谷駅北口から徒歩3分")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "shopName"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void postSignup_duplicateEmail_reRendersWithFieldError() throws Exception {
        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(mock(User.class)));

        mockMvc.perform(post("/signup")
                        .param("role", "BUYER")
                        .param("email", "dup@example.com")
                        .param("password", "password123")
                        .param("displayName", "テスト買い手")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "email"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void postSignup_blankRequiredFields_reRendersWithValidationErrors() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("role", "BUYER")
                        .param("email", "")
                        .param("password", "")
                        .param("displayName", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "email", "password", "displayName"));
    }

    @Test
    void postSignup_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("role", "BUYER")
                        .param("email", "nocrsf@example.com")
                        .param("password", "password123")
                        .param("displayName", "テスト"))
                .andExpect(status().isForbidden());
    }
}
