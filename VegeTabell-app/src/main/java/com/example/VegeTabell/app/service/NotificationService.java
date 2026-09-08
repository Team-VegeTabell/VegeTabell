package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Notification;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.NotificationType;
import com.example.VegeTabell.app.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void create(User recipient, NotificationType type, String title, String body,
                        Product product, Reservation reservation) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setProduct(product);
        notification.setReservation(reservation);
        notificationRepository.save(notification);
    }
}
