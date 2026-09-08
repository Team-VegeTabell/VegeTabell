package com.example.VegeTabell.app.dto;

import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.type.ReservationStatus;

/**
 * マイページ（買い手の予約履歴）の表示用モデル。
 */
public record MyPageReservationView(
        Long id,
        String productName,
        String productImageUrl,
        String shopName,
        int quantity,
        int totalPrice,
        String pickupWindowLabel,
        String statusLabel
) {

    public static MyPageReservationView from(Reservation reservation) {
        return new MyPageReservationView(
                reservation.getId(),
                reservation.getProduct().getName(),
                reservation.getProduct().getImageUrl(),
                reservation.getProduct().getShop().getShopName(),
                reservation.getQuantity(),
                reservation.getTotalPrice(),
                PickupWindowPresenter.label(reservation.getPickupStartAt(), reservation.getPickupEndAt()),
                statusLabel(reservation.getStatus())
        );
    }

    private static String statusLabel(ReservationStatus status) {
        return switch (status) {
            case RESERVED -> "予約中";
            case COMPLETED -> "受け取り済み";
            case CANCELED -> "キャンセル済み";
        };
    }
}
