package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.form.SignupForm;
import com.example.VegeTabell.app.repository.ShopRepository;
import com.example.VegeTabell.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SignupService signupService() {
        return new SignupService(userRepository, shopRepository, passwordEncoder);
    }

    private SignupForm buyerForm() {
        SignupForm form = new SignupForm();
        form.setRole(UserRole.BUYER);
        form.setEmail("buyer@example.com");
        form.setPassword("password123");
        form.setDisplayName("テスト買い手");
        return form;
    }

    private SignupForm sellerForm() {
        SignupForm form = new SignupForm();
        form.setRole(UserRole.SELLER);
        form.setEmail("seller@example.com");
        form.setPassword("password123");
        form.setDisplayName("テスト店主");
        form.setShopName("大地の恵み 八百屋");
        form.setAddress("東京都世田谷区1-1-1");
        form.setLatitude(new BigDecimal("35.646100"));
        form.setLongitude(new BigDecimal("139.653400"));
        form.setPickupNote("世田谷駅北口から徒歩3分");
        return form;
    }

    @Test
    void register_buyer_savesUserOnly() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");

        signupService().register(buyerForm());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("buyer@example.com", saved.getEmail());
        assertEquals("encoded-hash", saved.getPasswordHash());
        assertNotEquals("password123", saved.getPasswordHash());
        assertEquals(UserRole.BUYER, saved.getRole());

        verify(shopRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void register_seller_savesUserAndShopLinkedTogether() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");

        signupService().register(sellerForm());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        ArgumentCaptor<Shop> shopCaptor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(shopCaptor.capture());
        Shop savedShop = shopCaptor.getValue();

        assertEquals(savedUser, savedShop.getUser());
        assertEquals("大地の恵み 八百屋", savedShop.getShopName());
        assertEquals("東京都世田谷区1-1-1", savedShop.getAddress());
        assertEquals(new BigDecimal("35.646100"), savedShop.getLatitude());
        assertEquals(new BigDecimal("139.653400"), savedShop.getLongitude());
        assertEquals("世田谷駅北口から徒歩3分", savedShop.getPickupNote());
    }

    @Test
    void emailExists_true() {
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(mock(User.class)));
        assertTrue(signupService().emailExists("taken@example.com"));
    }

    @Test
    void emailExists_false() {
        when(userRepository.findByEmail("free@example.com")).thenReturn(Optional.empty());
        assertFalse(signupService().emailExists("free@example.com"));
    }
}
