package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Notification;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.CanceledBy;
import com.example.VegeTabell.app.entity.type.NotificationType;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.repository.NotificationRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User seller(long userId) {
        User seller = new User();
        seller.setId(userId);
        seller.setDisplayName("テスト店主");
        return seller;
    }

    private Product product(int remainingQuantity) {
        Shop shop = new Shop();
        shop.setId(20L);
        shop.setUser(seller(99L));

        Product product = new Product();
        product.setId(1L);
        product.setShop(shop);
        product.setName("トマト袋詰め");
        product.setRemainingQuantity(remainingQuantity);
        product.setRescuePrice(240);
        product.setStatus(ProductStatus.ON_SALE);
        product.setExpiryAt(Instant.now().plus(3, ChronoUnit.HOURS));
        return product;
    }

    private User buyer(long userId) {
        User buyer = new User();
        buyer.setId(userId);
        buyer.setDisplayName("テスト買い手");
        return buyer;
    }

    @Test
    void reserve_decrementsStockAndKeepsOnSale_whenStockRemains() {
        Product product = product(3);
        User buyer = buyer(9L);
        when(reservationRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation reservation = reservationService.reserve(product, buyer, 2);

        assertEquals(1, product.getRemainingQuantity());
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
        assertEquals(480, reservation.getTotalPrice());
        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
        verify(productRepository).save(product);
    }

    @Test
    void reserve_notifiesBuyerAndSeller() {
        Product product = product(3);
        User buyer = buyer(9L);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserve(product, buyer, 2);

        List<Notification> notifications = captor.getAllValues();
        assertEquals(2, notifications.size());
        assertEquals(NotificationType.RESERVATION_CONFIRMED, notifications.get(0).getType());
        assertEquals(buyer, notifications.get(0).getUser());
        assertEquals(NotificationType.NEW_RESERVATION, notifications.get(1).getType());
        assertEquals(product.getShop().getUser(), notifications.get(1).getUser());
    }

    @Test
    void reserve_lastUnit_marksProductSoldOut() {
        Product product = product(2);
        User buyer = buyer(9L);
        when(reservationRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserve(product, buyer, 2);

        assertEquals(0, product.getRemainingQuantity());
        assertEquals(ProductStatus.SOLD_OUT, product.getStatus());
    }

    @Test
    void cancel_restoresStockAndReopensSoldOutProduct() {
        Product product = product(0);
        product.setStatus(ProductStatus.SOLD_OUT);

        Reservation reservation = new Reservation();
        reservation.setProduct(product);
        reservation.setBuyer(buyer(9L));
        reservation.setQuantity(2);
        reservation.setStatus(ReservationStatus.RESERVED);

        reservationService.cancel(reservation, CanceledBy.BUYER);

        assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
        assertEquals(CanceledBy.BUYER, reservation.getCanceledBy());
        assertEquals(2, product.getRemainingQuantity());
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
    }

    @Test
    void cancel_byBuyer_notifiesSeller() {
        Product product = product(0);
        Reservation reservation = new Reservation();
        reservation.setProduct(product);
        reservation.setBuyer(buyer(9L));
        reservation.setQuantity(1);
        reservation.setStatus(ReservationStatus.RESERVED);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.cancel(reservation, CanceledBy.BUYER);

        assertEquals(1, captor.getAllValues().size());
        Notification notification = captor.getValue();
        assertEquals(NotificationType.RESERVATION_CANCELED, notification.getType());
        assertEquals(product.getShop().getUser(), notification.getUser());
    }

    @Test
    void cancel_bySeller_notifiesBuyer() {
        Product product = product(0);
        User buyer = buyer(9L);
        Reservation reservation = new Reservation();
        reservation.setProduct(product);
        reservation.setBuyer(buyer);
        reservation.setQuantity(1);
        reservation.setStatus(ReservationStatus.RESERVED);
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.cancel(reservation, CanceledBy.SELLER);

        verify(notificationRepository, times(1)).save(any());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(buyer, captor.getValue().getUser());
    }

    @Test
    void cancel_expiredProduct_doesNotReopenToOnSale() {
        Product product = product(0);
        product.setStatus(ProductStatus.SOLD_OUT);
        product.setExpiryAt(Instant.now().minus(1, ChronoUnit.HOURS));

        Reservation reservation = new Reservation();
        reservation.setProduct(product);
        reservation.setBuyer(buyer(9L));
        reservation.setQuantity(1);
        reservation.setStatus(ReservationStatus.RESERVED);

        reservationService.cancel(reservation, CanceledBy.SELLER);

        assertEquals(ProductStatus.SOLD_OUT, product.getStatus());
        assertEquals(1, product.getRemainingQuantity());
    }

    @Test
    void reserve_setsPickupWindow_fromNowToProductExpiry() {
        Product product = product(3);
        User buyer = buyer(9L);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserve(product, buyer, 1);

        Reservation saved = captor.getValue();
        assertTrue(!saved.getPickupStartAt().isAfter(Instant.now()));
        assertEquals(product.getExpiryAt(), saved.getPickupEndAt());
    }
}
