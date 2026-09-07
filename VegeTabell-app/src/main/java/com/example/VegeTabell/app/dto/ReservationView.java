package com.example.VegeTabell.app.dto;

import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.type.ReservationStatus;

/**
 * 予約内容確認画面（買い手向け）の表示用モデル。
 */
public record ReservationView(
        Long id,
        String productName,
        String productImageUrl,
        String shopName,
        String shopAddress,
        String pickupNote,
        int quantity,
        int totalPrice,
        String pickupWindowLabel,
        boolean cancelable,
        String statusLabel
) {

    public static ReservationView from(Reservation reservation) {
        return new ReservationView(
                reservation.getId(),
                reservation.getProduct().getName(),
                reservation.getProduct().getImageUrl(),
                reservation.getProduct().getShop().getShopName(),
                reservation.getProduct().getShop().getAddress(),
                reservation.getProduct().getShop().getPickupNote(),
                reservation.getQuantity(),
                reservation.getTotalPrice(),
                PickupWindowPresenter.label(reservation.getPickupStartAt(), reservation.getPickupEndAt()),
                reservation.getStatus() == ReservationStatus.RESERVED,
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
