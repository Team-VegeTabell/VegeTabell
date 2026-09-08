package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.NotificationType;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.repository.NotificationRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BatchService batchService;

    private Product product(long id, ProductStatus status) {
        Shop shop = new Shop();
        shop.setId(10L);
        User seller = new User();
        seller.setId(20L);
        shop.setUser(seller);

        Product product = new Product();
        product.setId(id);
        product.setName("トマト袋詰め");
        product.setShop(shop);
        product.setStatus(status);
        return product;
    }

    private Reservation reservation(long id, User buyer, Product product) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setBuyer(buyer);
        reservation.setProduct(product);
        reservation.setStatus(ReservationStatus.RESERVED);
        return reservation;
    }

    @Test
    void expireOverdueProducts_marksOverdueProductsAsExpired() {
        Product product = product(1L, ProductStatus.ON_SALE);
        when(productRepository.findByStatusInAndExpiryAtBefore(any(), any()))
                .thenReturn(List.of(product));

        batchService.expireOverdueProducts();

        assertEquals(ProductStatus.EXPIRED, product.getStatus());
        verify(productRepository).save(product);
    }

    @Test
    void completeOverdueReservations_marksOverdueReservationsAsCompleted() {
        Reservation reservation = reservation(1L, new User(), product(1L, ProductStatus.ON_SALE));
        when(reservationRepository.findByStatusAndPickupEndAtBefore(eq(ReservationStatus.RESERVED), any()))
                .thenReturn(List.of(reservation));

        batchService.completeOverdueReservations();

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void sendPickupReminders_notifiesBuyer_whenNotAlreadyNotified() {
        User buyer = new User();
        buyer.setId(9L);
        Product product = product(1L, ProductStatus.ON_SALE);
        Reservation reservation = reservation(5L, buyer, product);
        when(reservationRepository.findByStatusAndPickupEndAtBetween(eq(ReservationStatus.RESERVED), any(), any()))
                .thenReturn(List.of(reservation));
        when(notificationRepository.existsByReservationIdAndType(5L, NotificationType.PICKUP_REMINDER))
                .thenReturn(false);

        batchService.sendPickupReminders();

        verify(notificationService).create(eq(buyer), eq(NotificationType.PICKUP_REMINDER),
                any(), any(), eq(product), eq(reservation));
    }

    @Test
    void sendPickupReminders_skipsReservation_whenAlreadyNotified() {
        User buyer = new User();
        Product product = product(1L, ProductStatus.ON_SALE);
        Reservation reservation = reservation(5L, buyer, product);
        when(reservationRepository.findByStatusAndPickupEndAtBetween(eq(ReservationStatus.RESERVED), any(), any()))
                .thenReturn(List.of(reservation));
        when(notificationRepository.existsByReservationIdAndType(5L, NotificationType.PICKUP_REMINDER))
                .thenReturn(true);

        batchService.sendPickupReminders();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendStockExpiringWarnings_notifiesShopOwner_whenNotAlreadyNotified() {
        Product product = product(2L, ProductStatus.ON_SALE);
        when(productRepository.findByStatusAndExpiryAtBetween(eq(ProductStatus.ON_SALE), any(), any()))
                .thenReturn(List.of(product));
        when(notificationRepository.existsByProductIdAndType(2L, NotificationType.STOCK_EXPIRING_WARNING))
                .thenReturn(false);

        batchService.sendStockExpiringWarnings();

        verify(notificationService).create(eq(product.getShop().getUser()),
                eq(NotificationType.STOCK_EXPIRING_WARNING), any(), any(), eq(product), isNull());
    }

    @Test
    void sendStockExpiringWarnings_skipsProduct_whenAlreadyNotified() {
        Product product = product(2L, ProductStatus.ON_SALE);
        when(productRepository.findByStatusAndExpiryAtBetween(eq(ProductStatus.ON_SALE), any(), any()))
                .thenReturn(List.of(product));
        when(notificationRepository.existsByProductIdAndType(2L, NotificationType.STOCK_EXPIRING_WARNING))
                .thenReturn(true);

        batchService.sendStockExpiringWarnings();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any());
    }
}
