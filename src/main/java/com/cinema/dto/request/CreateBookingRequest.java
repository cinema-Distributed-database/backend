package com.cinema.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    @NotNull(message = "Showtime ID không được để trống")
    private String showtimeId;

    @NotNull(message = "Thông tin khách hàng không được để trống")
    @Valid
    private CustomerInfoRequest customerInfo;

    @NotEmpty(message = "Danh sách ghế không được để trống")
    private List<String> seats;

    @Valid
    private List<TicketTypeRequest> ticketTypes;

    @Valid
    private List<ConcessionRequest> concessions; // Danh sách đồ ăn thức uống kèm theo

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfoRequest {
        @NotEmpty(message = "Họ tên không được để trống")
        private String fullName;

        @NotEmpty(message = "Số điện thoại không được để trống")
        private String phone;

        private String email; // Tùy chọn
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeRequest {
        @NotEmpty(message = "Loại vé không được để trống")
        private String type; // Ví dụ: "Người Lớn", "Trẻ Em"

        @NotNull(message = "Số lượng vé không được để trống")
        private Integer quantity;

        @NotNull(message = "Giá mỗi vé không được để trống")
        private Long pricePerTicket; // Giá tại thời điểm đặt vé
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcessionRequest {
        @NotNull(message = "Item ID của concession không được để trống")
        private String itemId; // ID của Concession model

        @NotNull(message = "Số lượng không được để trống")
        private Integer quantity;
        
        // Tên và giá có thể lấy từ DB dựa trên itemId khi tạo booking
    }
}