package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rooms")
public class Room {
    @Id
    private String id;
    
    private String cinemaId;
    
    private String roomNumber;
    private String name;
    private String type;
    private Integer capacity;
    private String status;
    private List<String> features;
    private SeatMap seatMap;
    private LocalDateTime lastMaintenance;
    private LocalDateTime nextMaintenanceScheduled;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatMap {
        private List<SeatRow> rows;
        private SeatMetadata metadata;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatRow {
        private String id;
        private List<SeatInfo> seats;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
        private String id;
        private String type;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatMetadata {
        private Integer totalSeats;
        private Integer totalSellableSeats;
        private Map<String, SeatType> seatTypes;
        private Screen screen;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatType {
        private Long basePrice;
        private String color;
        private String label;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Screen {
        private String label;
        private String position;
    }
}