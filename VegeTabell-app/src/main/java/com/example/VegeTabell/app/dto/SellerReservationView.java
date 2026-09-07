package com.example.VegeTabell.app.dto;

import com.example.VegeTabell.app.entity.Reservation;

/**
 * 売り手ダッシュボードの「予約一覧」セクションの表示用モデル。
 */
public record SellerReservationView(
        Long id,
        String productName,
        String buyerDisplayName,
        int quantity,
        String pickupWindowLabel
) {

    public static SellerReservationView from(Reservation reservation) {
        return new SellerReservationView(
                reservation.getId(),
                reservation.getProduct().getName(),
                reservation.getBuyer().getDisplayName(),
                reservation.getQuantity(),
                PickupWindowPresenter.label(reservation.getPickupStartAt(), reservation.getPickupEndAt())
        );
    }
}
