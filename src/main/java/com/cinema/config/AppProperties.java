package com.cinema.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cinema")
public class AppProperties {
    
    private SeatHold seatHold = new SeatHold();
    private Booking booking = new Booking();
    
    @Data
    public static class SeatHold {
        private int expiryMinutes = 10;
    }
    
    @Data
    public static class Booking {
        private ConfirmationCode confirmationCode = new ConfirmationCode();
        
        @Data
        public static class ConfirmationCode {
            private String prefix = "CINESTAR";
        }
    }
}