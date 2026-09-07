package com.example.VegeTabell.app.entity;

import com.example.VegeTabell.app.entity.type.CanceledBy;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "pickup_start_at", nullable = false)
    private Instant pickupStartAt;

    @Column(name = "pickup_end_at", nullable = false)
    private Instant pickupEndAt;

    @Column(name = "status", nullable = false, length = 10)
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "canceled_by", length = 10)
    private CanceledBy canceledBy;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @CreationTimestamp
    @Column(name = "reserved_at", nullable = false, updatable = false)
    private Instant reservedAt;
}
