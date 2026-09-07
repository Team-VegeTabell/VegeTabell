package com.example.VegeTabell.app.entity.type;

public enum ReservationStatus {
    RESERVED("reserved"),
    COMPLETED("completed"),
    CANCELED("canceled");

    private final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReservationStatus fromValue(String value) {
        for (ReservationStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReservationStatus value: " + value);
    }
}
