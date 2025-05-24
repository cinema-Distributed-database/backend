package com.cinema.dto.response;

import com.cinema.model.Booking;
import com.cinema.model.Cinema;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Showtime; // Cần thiết nếu muốn lấy showDateTime từ showtime được lookup
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingAggregatedDetailsDto {
    private String id;
    private Booking.CustomerInfo customerInfo;
    private String showtimeId; // Vẫn giữ showtimeId gốc

    // Thông tin được join từ các collection khác
    private String movieTitle;
    private String cinemaName;
    private String roomName;
    private LocalDateTime showDateTime; // Lấy từ showtime được join

    private LocalDateTime bookingTime;
    private List<String> seats;
    private List<Booking.TicketType> ticketTypes;
    private List<Booking.ConcessionItem> concessions;
    private Long totalPrice;
    private String paymentStatus;
    private String paymentMethod;
    private String confirmationCode;
    private LocalDateTime createdAt;

    // Thông tin bổ sung từ các document được join (nếu cần)
    // Ví dụ: movie có thể có poster, cinema có thể có address
    private Movie movieDetails; // Toàn bộ object Movie nếu cần
    private Cinema cinemaDetails; // Toàn bộ object Cinema nếu cần
    private Room roomDetails; // Toàn bộ object Room nếu cần
    private Showtime showtimeDetails; // Toàn bộ object Showtime nếu cần

    // Constructor hoặc factory method để chuyển đổi từ Booking và các object đã join
    public static BookingAggregatedDetailsDto fromBookingAndAggregatedData(
            Booking booking, Movie movie, Cinema cinema, Room room, Showtime showtime) {
        BookingAggregatedDetailsDto dto = new BookingAggregatedDetailsDto();
        dto.setId(booking.getId());
        dto.setCustomerInfo(booking.getCustomerInfo());
        dto.setShowtimeId(booking.getShowtimeId());

        dto.setMovieTitle(movie != null ? movie.getTitle() : "N/A");
        dto.setCinemaName(cinema != null ? cinema.getName() : "N/A");
        dto.setRoomName(room != null ? room.getName() : "N/A");
        dto.setShowDateTime(showtime != null ? showtime.getShowDateTime() : null);

        dto.setBookingTime(booking.getBookingTime());
        dto.setSeats(booking.getSeats());
        dto.setTicketTypes(booking.getTicketTypes());
        dto.setConcessions(booking.getConcessions());
        dto.setTotalPrice(booking.getTotalPrice());

        if (booking.getPaymentStatus() != null) {
            dto.setPaymentStatus(booking.getPaymentStatus().name());
        }
        if (booking.getPaymentMethod() != null) {
            dto.setPaymentMethod(booking.getPaymentMethod().name());
        }
        dto.setConfirmationCode(booking.getConfirmationCode());
        dto.setCreatedAt(booking.getCreatedAt());

        // Gán các object đầy đủ nếu cần
        dto.setMovieDetails(movie);
        dto.setCinemaDetails(cinema);
        dto.setRoomDetails(room);
        dto.setShowtimeDetails(showtime);

        return dto;
    }
}