package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
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
    private String status;
    private Map<String, SeatStatus> seatStatus;
    
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
        private String status; // available, holding, booked, unavailable
        private LocalDateTime holdStartedAt;
        private String bookingId;
    }
}