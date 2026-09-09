package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.ReservationRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationRepository reservationRepository;

    private User buyer(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("buyer@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.BUYER);
        user.setDisplayName("テスト買い手");
        return user;
    }

    private User seller(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("seller@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.SELLER);
        user.setDisplayName("テスト店主");
        return user;
    }

    private Reservation reservation(long id, User buyer) {
        Shop shop = new Shop();
        shop.setShopName("テスト店舗");

        Product product = new Product();
        product.setName("トマト袋詰め");
        product.setShop(shop);

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setBuyer(buyer);
        reservation.setProduct(product);
        reservation.setQuantity(2);
        reservation.setTotalPrice(480);
        reservation.setPickupStartAt(Instant.now());
        reservation.setPickupEndAt(Instant.now().plusSeconds(3600));
        reservation.setStatus(ReservationStatus.RESERVED);
        return reservation;
    }

    @Test
    void mypage_showsBuyersReservationsOrderedByReservedAtDesc() throws Exception {
        User buyer = buyer(1L);
        when(reservationRepository.findByBuyerIdOrderByReservedAtDesc(1L))
                .thenReturn(List.of(reservation(10L, buyer)));

        mockMvc.perform(get("/mypage").with(user(new CustomUserDetails(buyer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("トマト袋詰め")))
                .andExpect(content().string(containsString("テスト店舗")));
    }

    @Test
    void mypage_emptyList_showsEmptyState() throws Exception {
        User buyer = buyer(1L);
        when(reservationRepository.findByBuyerIdOrderByReservedAtDesc(1L)).thenReturn(List.of());

        mockMvc.perform(get("/mypage").with(user(new CustomUserDetails(buyer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("まだ予約はありません")));
    }

    @Test
    void mypage_showsLogoutButton() throws Exception {
        User buyer = buyer(1L);
        when(reservationRepository.findByBuyerIdOrderByReservedAtDesc(1L)).thenReturn(List.of());

        mockMvc.perform(get("/mypage").with(user(new CustomUserDetails(buyer))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        containsString("action=\"/logout\""),
                        containsString("ログアウト")
                )));
    }

    @Test
    void mypage_sellerPrincipal_isForbidden() throws Exception {
        mockMvc.perform(get("/mypage").with(user(new CustomUserDetails(seller(2L)))))
                .andExpect(status().isForbidden());
    }
}
