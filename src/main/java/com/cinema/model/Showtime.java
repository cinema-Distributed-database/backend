package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.cinema.enums.SeatState;
import com.cinema.enums.ShowtimeStatus;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "showtimes")
public class Showtime {
    @Id
    private String id;
    
    private String movieId;
    
    private String cinemaId;
    
    private String roomId;
    
    private LocalDateTime showDateTime;
    
    private String screenType;
    private PricingTiers pricingTiers;
    private Integer totalSeats;
    private Integer availableSeats;
    
    // Sử dụng String thay vì enum để tương thích với dữ liệu hiện tại
    @Field("status")
    private ShowtimeStatus status;
    
    private Map<String, SeatStatus> seatStatus;
    private boolean hasHoldingSeats;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingTiers {
        private Long standard;
        private Long vip;
        private Long couple;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatStatus {
        private SeatState status;// available, holding, booked, unavailable
        private LocalDateTime holdStartedAt;
        private String bookingId;
    }
}