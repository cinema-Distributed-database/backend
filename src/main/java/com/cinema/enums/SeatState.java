package com.cinema.enums;

public enum SeatState {
    AVAILABLE("available"),
    HOLDING("holding"),
    BOOKED("booked"),
    UNAVAILABLE("unavailable"); // Nếu có trạng thái này

    private final String value;

    SeatState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // Optional: phương thức để chuyển đổi từ String sang Enum
    public static SeatState fromValue(String value) {
        for (SeatState state : values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown seat state: " + value);
    }
}