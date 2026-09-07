package com.example.VegeTabell.app.entity.type;

public enum CanceledBy {
    BUYER("buyer"),
    SELLER("seller");

    private final String value;

    CanceledBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CanceledBy fromValue(String value) {
        for (CanceledBy canceledBy : values()) {
            if (canceledBy.value.equals(value)) {
                return canceledBy;
            }
        }
        throw new IllegalArgumentException("Unknown CanceledBy value: " + value);
    }
}
