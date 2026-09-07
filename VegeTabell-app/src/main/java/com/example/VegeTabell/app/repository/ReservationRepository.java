package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
