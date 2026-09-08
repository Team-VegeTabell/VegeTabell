package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Notification;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.NotificationType;
import com.example.VegeTabell.app.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void create_savesNotificationWithGivenFields() {
        User recipient = new User();
        recipient.setId(1L);
        Product product = new Product();
        product.setId(2L);
        Reservation reservation = new Reservation();
        reservation.setId(3L);

        notificationService.create(recipient, NotificationType.PICKUP_REMINDER,
                "受け取り期限が近づいています", "本文", product, reservation);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(recipient, saved.getUser());
        assertEquals(NotificationType.PICKUP_REMINDER, saved.getType());
        assertEquals("受け取り期限が近づいています", saved.getTitle());
        assertEquals("本文", saved.getBody());
        assertEquals(product, saved.getProduct());
        assertEquals(reservation, saved.getReservation());
    }
}
