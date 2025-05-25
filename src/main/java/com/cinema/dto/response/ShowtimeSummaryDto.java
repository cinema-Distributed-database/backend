package com.cinema.dto.response;

import com.cinema.enums.ShowtimeStatus; // Import enum
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSummaryDto {
    private String id;
    private String movieId;
    private String cinemaId;
    private String roomId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // Định dạng ngày giờ cho JSON output
    private LocalDateTime showDateTime;

    private String screenType;
    private Integer totalSeats;
    private Integer availableSeats;
    private ShowtimeStatus status; // Sử dụng trực tiếp kiểu enum ShowtimeStatus
    // Không bao gồm: private Map<String, Showtime.SeatStatus> seatStatus;
    // Không bao gồm: private boolean hasHoldingSeats;
    // Không bao gồm: private Showtime.PricingTiers pricingTiers; (Tùy chọn, có thể thêm nếu cần)
}