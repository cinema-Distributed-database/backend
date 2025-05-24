package com.cinema.enums;

public enum ShowtimeStatus {
    ACTIVE("active"),
    INACTIVE("inactive"), // Hoặc các trạng thái khác như CANCELED, FINISHED
    COMING_SOON("coming-soon"), // Nếu có
    ENDED("ended");


    private final String value;

    ShowtimeStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
     public static ShowtimeStatus fromValue(String value) {
        for (ShowtimeStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown showtime status: " + value);
    }
}