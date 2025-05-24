package com.cinema.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatRequest {
    @NotNull(message = "Showtime ID không được để trống")
    private String showtimeId;

    @NotEmpty(message = "Danh sách ghế không được để trống")
    private List<String> seatIds;

    private String customerPhone; // Tùy chọn, để theo dõi session giữ ghế
}