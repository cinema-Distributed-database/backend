package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;
    
    private CustomerInfo customerInfo;
    
    private String showtimeId;
    
    private LocalDateTime bookingTime;
    private List<String> seats;
    private List<TicketType> ticketTypes;
    private List<Concession> concessions;
    private Long totalPrice;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    
    private String confirmationCode;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String fullName;
        private String phone;
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketType {
        private String type;
        private Integer quantity;
        private Long pricePerTicket;
        private Long subtotal;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Concession {
        private String itemId;
        private String name;
        private Integer quantity;
        private Long price;
    }
}