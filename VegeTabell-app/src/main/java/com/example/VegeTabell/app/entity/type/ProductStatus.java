package com.example.VegeTabell.app.entity.type;

public enum ProductStatus {
    ON_SALE("on_sale"),
    SOLD_OUT("sold_out"),
    EXPIRED("expired");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProductStatus fromValue(String value) {
        for (ProductStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ProductStatus value: " + value);
    }
}
