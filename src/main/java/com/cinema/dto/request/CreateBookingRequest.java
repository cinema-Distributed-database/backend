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
public class CreateBookingRequest { //
    @NotNull(message = "Showtime ID không được để trống") //
    private String showtimeId; //

    @NotNull(message = "Thông tin khách hàng không được để trống") //
    @Valid //
    private CustomerInfoRequest customerInfo; //

    @NotEmpty(message = "Danh sách ghế không được để trống") //
    private List<String> seats; //

    @Valid //
    private List<TicketTypeRequest> ticketTypes; //

    @Valid //
    private List<ConcessionItemRequest> concessions; // *** UPDATED *** Đổi tên để rõ ràng hơn

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfoRequest { //
        @NotEmpty(message = "Họ tên không được để trống") //
        private String fullName; //

        @NotEmpty(message = "Số điện thoại không được để trống") //
        private String phone; //

        private String email; //
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeRequest { //
        @NotEmpty(message = "Loại vé không được để trống") //
        private String type; //

        @NotNull(message = "Số lượng vé không được để trống") //
        private Integer quantity; //

        @NotNull(message = "Giá mỗi vé không được để trống") //
        private Long pricePerTicket; //
    }

    // *** UPDATED ***
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcessionItemRequest { // Đổi tên từ ConcessionRequest
        @NotNull(message = "Item ID của concession không được để trống") //
        private String itemId; //

        @NotNull(message = "Số lượng không được để trống") //
        private Integer quantity; //
    }
    // *** END UPDATED ***
}