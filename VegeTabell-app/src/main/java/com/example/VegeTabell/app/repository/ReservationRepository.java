package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByProductId(Long productId);

    List<Reservation> findByProductShopIdAndStatusOrderByReservedAtDesc(Long shopId, ReservationStatus status);

    List<Reservation> findByStatusAndPickupEndAtBefore(ReservationStatus status, Instant threshold);

    List<Reservation> findByStatusAndPickupEndAtBetween(ReservationStatus status, Instant from, Instant to);

    List<Reservation> findByBuyerIdOrderByReservedAtDesc(Long buyerId);

    @Query("SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r "
            + "WHERE r.product.shop.id = :shopId AND r.status = :status")
    int sumTotalPriceByProductShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") ReservationStatus status);
}
