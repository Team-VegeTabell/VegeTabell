package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Notification;
import com.example.VegeTabell.app.entity.type.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByReservationIdAndType(Long reservationId, NotificationType type);

    boolean existsByProductIdAndType(Long productId, NotificationType type);
}
