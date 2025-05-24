package com.cinema.dto.response;

import com.cinema.model.Showtime; // Sử dụng Showtime.SeatStatus
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusDto {
    private String showtimeId;
    private Map<String, Showtime.SeatStatus> seatStatus;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer holdingSeats;
    private Integer bookedSeats;
}