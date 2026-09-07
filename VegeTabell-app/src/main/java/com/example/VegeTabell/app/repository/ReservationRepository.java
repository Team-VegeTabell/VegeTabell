package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByProductId(Long productId);

    List<Reservation> findByProductShopIdAndStatusOrderByReservedAtDesc(Long shopId, ReservationStatus status);
}
