package com.example.VegeTabell.app.entity.type;

public enum NotificationType {
    RESERVATION_CONFIRMED("reservation_confirmed"),
    PICKUP_REMINDER("pickup_reminder"),
    NEW_PRODUCT_NEARBY("new_product_nearby"),
    RESERVATION_CANCELED("reservation_canceled"),
    NEW_RESERVATION("new_reservation"),
    STOCK_EXPIRING_WARNING("stock_expiring_warning"),
    PICKUP_COMPLETED("pickup_completed");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationType fromValue(String value) {
        for (NotificationType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationType value: " + value);
    }
}
