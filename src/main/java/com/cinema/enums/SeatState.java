package com.cinema.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SeatState {
    AVAILABLE("available"),
    HOLDING("holding"),
    BOOKED("booked"),
    UNAVAILABLE("unavailable");

    private final String value;

    SeatState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SeatState fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (SeatState state : values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown seat state: " + value);
    }
}