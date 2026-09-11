package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.CanceledBy;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import com.example.VegeTabell.app.repository.ShopRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import com.example.VegeTabell.app.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ReservationRepository reservationRepository;

    @MockitoBean
    private ShopRepository shopRepository;

    @MockitoBean
    private ReservationService reservationService;

    private User buyer(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("buyer" + id + "@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.BUYER);
        user.setDisplayName("買い手" + id);
        return user;
    }

    private UserDetails buyerPrincipal(long id) {
        return new CustomUserDetails(buyer(id));
    }

    private UserDetails sellerPrincipal(long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("seller@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.SELLER);
        user.setDisplayName("テスト店主");
        return new CustomUserDetails(user);
    }

    private Product onSaleProduct(int remainingQuantity) {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setShopName("大地の恵み 八百屋");
        shop.setAddress("東京都世田谷区1-1-1");

        Product product = new Product();
        product.setId(100L);
        product.setShop(shop);
        product.setName("トマト袋詰め");
        product.setNormalPrice(580);
        product.setRescuePrice(240);
        product.setTotalQuantity(3);
        product.setRemainingQuantity(remainingQuantity);
        product.setExpiryAt(Instant.now().plus(3, ChronoUnit.HOURS));
        product.setStatus(ProductStatus.ON_SALE);
        return product;
    }

    private Reservation reservation(Product product, User buyer, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(500L);
        reservation.setProduct(product);
        reservation.setBuyer(buyer);
        reservation.setQuantity(1);
        reservation.setTotalPrice(product.getRescuePrice());
        reservation.setPickupStartAt(Instant.now());
        reservation.setPickupEndAt(product.getExpiryAt());
        reservation.setStatus(status);
        return reservation;
    }

    @Test
    void postReserve_happyPath_redirectsToConfirmation() throws Exception {
        Product product = onSaleProduct(3);
        User buyer = buyer(1L);
        Reservation saved = reservation(product, buyer, ReservationStatus.RESERVED);

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(reservationService.reserve(eq(product), any(User.class), eq(2))).thenReturn(saved);

        mockMvc.perform(post("/products/{id}/reservations", 100L)
                        .with(user(buyerPrincipal(1L)))
                        .param("quantity", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/reservations/500"));

        verify(reservationService).reserve(eq(product), any(User.class), eq(2));
    }

    @Test
    void postReserve_quantityExceedsStock_reRendersWithFieldError() throws Exception {
        Product product = onSaleProduct(1);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        mockMvc.perform(post("/products/{id}/reservations", 100L)
                        .with(user(buyerPrincipal(1L)))
                        .param("quantity", "2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("products/detail"))
                .andExpect(model().attributeHasFieldErrors("reservationForm", "quantity"));

        verify(reservationService, never()).reserve(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void postReserve_productSoldOut_reRendersWithFieldError() throws Exception {
        Product product = onSaleProduct(0);
        product.setStatus(ProductStatus.SOLD_OUT);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        mockMvc.perform(post("/products/{id}/reservations", 100L)
                        .with(user(buyerPrincipal(1L)))
                        .param("quantity", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("reservationForm", "quantity"));
    }

    @Test
    void postReserve_productNotFound_returns404() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/products/{id}/reservations", 999L)
                        .with(user(buyerPrincipal(1L)))
                        .param("quantity", "1")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConfirm_owner_showsReservation() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(get("/reservations/{id}", 500L).with(user(buyerPrincipal(1L))))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/confirm"));
    }

    @Test
    void getConfirm_notOwner_isForbidden() throws Exception {
        Product product = onSaleProduct(2);
        User otherBuyer = buyer(2L);
        Reservation reservation = reservation(product, otherBuyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(get("/reservations/{id}", 500L).with(user(buyerPrincipal(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConfirm_notFound_returns404() throws Exception {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/reservations/{id}", 999L).with(user(buyerPrincipal(1L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void postComplete_byOwningBuyer_completesAndRedirectsToConfirmation() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(post("/reservations/{id}/complete", 500L)
                        .with(user(buyerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/reservations/500"));

        verify(reservationService).complete(reservation);
    }

    @Test
    void postComplete_byNonOwner_isForbidden() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(post("/reservations/{id}/complete", 500L)
                        .with(user(buyerPrincipal(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(reservationService, never()).complete(any());
    }

    @Test
    void postComplete_alreadyProcessed_doesNotCompleteAgain() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.CANCELED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(post("/reservations/{id}/complete", 500L)
                        .with(user(buyerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/reservations/500"));

        verify(reservationService, never()).complete(any());
    }

    @Test
    void postCancel_byOwningBuyer_cancelsAndRedirectsToConfirmation() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(post("/reservations/{id}/cancel", 500L)
                        .with(user(buyerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/reservations/500"));

        verify(reservationService).cancel(reservation, CanceledBy.BUYER);
    }

    @Test
    void postCancel_byOwningSeller_cancelsAndRedirectsToDashboard() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        Shop sellerShop = product.getShop();
        when(shopRepository.findByUserId(2L)).thenReturn(Optional.of(sellerShop));

        mockMvc.perform(post("/reservations/{id}/cancel", 500L)
                        .with(user(sellerPrincipal(2L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/seller/dashboard"));

        verify(reservationService).cancel(reservation, CanceledBy.SELLER);
    }

    @Test
    void postCancel_byUnrelatedUser_isForbidden() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.RESERVED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));
        when(shopRepository.findByUserId(3L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/reservations/{id}/cancel", 500L)
                        .with(user(buyerPrincipal(3L)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(reservationService, never()).cancel(any(), any());
    }

    @Test
    void postCancel_alreadyCanceled_doesNotCancelAgain() throws Exception {
        Product product = onSaleProduct(2);
        User buyer = buyer(1L);
        Reservation reservation = reservation(product, buyer, ReservationStatus.CANCELED);
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(post("/reservations/{id}/cancel", 500L)
                        .with(user(buyerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/reservations/500"));

        verify(reservationService, never()).cancel(any(), any());
    }
}
