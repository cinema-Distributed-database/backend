package com.cinema.dto.response;

import com.cinema.model.Booking; //
// Không cần import Enum ở đây nếu chỉ nhận String
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; //
import java.util.List; //

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailsDto {
    private String id; //
    private Booking.CustomerInfo customerInfo; //
    private String showtimeId; //
    private String movieTitle; //
    private String cinemaName; //
    private String roomName; //
    private LocalDateTime showDateTime; //
    private LocalDateTime bookingTime; //
    private List<String> seats; //
    private List<Booking.TicketType> ticketTypes; //
    private List<Booking.ConcessionItem> concessions; // *** UPDATED *** từ Booking.Concession
    private Long totalPrice; //
    private String paymentStatus; // Giữ là String cho client
    private String paymentMethod; // Giữ là String cho client
    private String confirmationCode; //
    private LocalDateTime createdAt; //

    public static BookingDetailsDto fromBooking(Booking booking, String movieTitle, String cinemaName, String roomName, LocalDateTime showDateTime) { //
        BookingDetailsDto dto = new BookingDetailsDto(); //
        dto.setId(booking.getId()); //
        dto.setCustomerInfo(booking.getCustomerInfo()); //
        dto.setShowtimeId(booking.getShowtimeId()); //
        dto.setMovieTitle(movieTitle); //
        dto.setCinemaName(cinemaName); //
        dto.setRoomName(roomName); //
        dto.setShowDateTime(showDateTime); //
        dto.setBookingTime(booking.getBookingTime()); //
        dto.setSeats(booking.getSeats()); //
        dto.setTicketTypes(booking.getTicketTypes()); //
        dto.setConcessions(booking.getConcessions()); //
        dto.setTotalPrice(booking.getTotalPrice()); //
        
        // *** UPDATED to handle Enum to String conversion ***
        if (booking.getPaymentStatus() != null) {
            dto.setPaymentStatus(booking.getPaymentStatus().name());
        }
        if (booking.getPaymentMethod() != null) {
            dto.setPaymentMethod(booking.getPaymentMethod().name());
        }
        // *** END UPDATED ***
        
        dto.setConfirmationCode(booking.getConfirmationCode()); //
        dto.setCreatedAt(booking.getCreatedAt()); //
        return dto; //
    }
}