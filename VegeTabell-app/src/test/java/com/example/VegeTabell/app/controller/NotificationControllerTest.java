package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.Notification;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.NotificationType;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.NotificationRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

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

    private Notification notification(long id, User owner, Reservation reservation) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUser(owner);
        notification.setType(NotificationType.RESERVATION_CONFIRMED);
        notification.setTitle("予約が確定しました");
        notification.setBody("トマト袋詰めを予約しました");
        notification.setReservation(reservation);
        notification.setCreatedAt(java.time.Instant.now());
        return notification;
    }

    private Reservation reservation(long id) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setProduct(new Product());
        return reservation;
    }

    @Test
    void list_buyer_showsOwnNotifications() throws Exception {
        User buyer = buyer(1L);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification(50L, buyer, null)));

        mockMvc.perform(get("/notifications").with(user(new CustomUserDetails(buyer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("予約が確定しました")));
    }

    @Test
    void read_ownNotificationWithoutReservation_redirectsToNotificationList() throws Exception {
        User buyer = buyer(1L);
        Notification notification = notification(50L, buyer, null);
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        mockMvc.perform(post("/notifications/{id}/read", 50L)
                        .with(user(new CustomUserDetails(buyer)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/notifications"));

        org.junit.jupiter.api.Assertions.assertTrue(notification.isRead());
    }

    @Test
    void read_buyerNotificationWithReservation_redirectsToReservationConfirm() throws Exception {
        User buyer = buyer(1L);
        Reservation reservation = reservation(200L);
        Notification notification = notification(50L, buyer, reservation);
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        mockMvc.perform(post("/notifications/{id}/read", 50L)
                        .with(user(new CustomUserDetails(buyer)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/reservations/200"));
    }

    @Test
    void read_sellerNotificationWithReservation_redirectsToSellerDashboard() throws Exception {
        User seller = seller(2L);
        Reservation reservation = reservation(200L);
        Notification notification = notification(60L, seller, reservation);
        when(notificationRepository.findById(60L)).thenReturn(Optional.of(notification));

        mockMvc.perform(post("/notifications/{id}/read", 60L)
                        .with(user(new CustomUserDetails(seller)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/seller/dashboard"));
    }

    @Test
    void read_notOwner_isForbidden() throws Exception {
        User owner = buyer(1L);
        User intruder = buyer(2L);
        Notification notification = notification(50L, owner, null);
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        mockMvc.perform(post("/notifications/{id}/read", 50L)
                        .with(user(new CustomUserDetails(intruder)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void read_notFound_returns404() throws Exception {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/notifications/{id}/read", 999L)
                        .with(user(new CustomUserDetails(buyer(1L))))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
