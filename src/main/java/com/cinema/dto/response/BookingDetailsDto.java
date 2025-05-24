package com.cinema.dto.response;

import com.cinema.model.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailsDto {
    private String id;
    private Booking.CustomerInfo customerInfo;
    private String showtimeId; // Có thể bổ sung thêm thông tin chi tiết về suất chiếu (phim, rạp, giờ)
    private String movieTitle;
    private String cinemaName;
    private String roomName;
    private LocalDateTime showDateTime;
    private LocalDateTime bookingTime;
    private List<String> seats;
    private List<Booking.TicketType> ticketTypes;
    private List<Booking.Concession> concessions;
    private Long totalPrice;
    private String paymentStatus;
    private String paymentMethod;
    private String confirmationCode;
    private LocalDateTime createdAt;

    // Phương thức factory để chuyển đổi từ Booking model
    public static BookingDetailsDto fromBooking(Booking booking, String movieTitle, String cinemaName, String roomName, LocalDateTime showDateTime) {
        BookingDetailsDto dto = new BookingDetailsDto();
        dto.setId(booking.getId());
        dto.setCustomerInfo(booking.getCustomerInfo());
        dto.setShowtimeId(booking.getShowtimeId());
        dto.setMovieTitle(movieTitle);
        dto.setCinemaName(cinemaName);
        dto.setRoomName(roomName);
        dto.setShowDateTime(showDateTime);
        dto.setBookingTime(booking.getBookingTime());
        dto.setSeats(booking.getSeats());
        dto.setTicketTypes(booking.getTicketTypes());
        dto.setConcessions(booking.getConcessions());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setPaymentStatus(booking.getPaymentStatus());
        dto.setPaymentMethod(booking.getPaymentMethod());
        dto.setConfirmationCode(booking.getConfirmationCode());
        dto.setCreatedAt(booking.getCreatedAt());
        return dto;
    }
}