package com.cinema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ShowtimeStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    COMING_SOON("coming-soon"),
    ENDED("ended");

    private final String value;

    ShowtimeStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ShowtimeStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ShowtimeStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown showtime status: " + value);
    }
}