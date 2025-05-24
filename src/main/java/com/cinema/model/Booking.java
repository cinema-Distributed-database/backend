package com.cinema.model;

import com.cinema.enums.PaymentMethodType; // Đảm bảo import
import com.cinema.enums.PaymentStatusType; // Đảm bảo import
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
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
    private String id; //
    
    private CustomerInfo customerInfo; //
    private String showtimeId; //
    private LocalDateTime bookingTime; //
    private List<String> seats; //
    private List<TicketType> ticketTypes; //
    
    // *** UPDATED ***
    private List<ConcessionItem> concessions; // Đổi tên từ Booking.Concession
    private PaymentStatusType paymentStatus;  // Đổi kiểu sang Enum
    private PaymentMethodType paymentMethod;  // Đổi kiểu sang Enum
    // *** END UPDATED ***

    private Long totalPrice; //
    private String paymentReference; // Mã tham chiếu của giao dịch thanh toán thành công (ví dụ: vnp_TxnRef)
    private String confirmationCode; //
    private LocalDateTime createdAt; //
    private LocalDateTime updatedAt; //
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo { //
        private String fullName; //
        private String phone; //
        private String email; //
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketType { //
        private String type; //
        private Integer quantity; //
        private Long pricePerTicket; //
        private Long subtotal; //
    }
    
    // *** UPDATED ***
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcessionItem { // Đổi tên từ Concession
        private String itemId; //
        private String name; //
        private Integer quantity; //
        private Long price; //
    }
    // *** END UPDATED ***
}