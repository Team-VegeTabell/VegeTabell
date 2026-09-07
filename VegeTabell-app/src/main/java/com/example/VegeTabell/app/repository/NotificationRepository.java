package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
