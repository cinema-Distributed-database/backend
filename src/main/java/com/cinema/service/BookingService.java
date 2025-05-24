package com.cinema.service;

import com.cinema.config.AppProperties; //
import com.cinema.dto.request.CreateBookingRequest; //
import com.cinema.dto.response.BookingAggregatedDetailsDto;
import com.cinema.dto.response.BookingDetailsDto; //
import com.cinema.enums.*;
import com.cinema.model.*; 
import com.cinema.repository.*; 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; //
import java.util.List; //
import java.util.Optional; //
import java.util.UUID; //
import java.util.stream.Collectors; //

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository; //
    private final ShowtimeRepository showtimeRepository; //
    private final MovieRepository movieRepository; //
    private final CinemaRepository cinemaRepository; //
    private final RoomRepository roomRepository; //
    private final ConcessionRepository concessionRepository; //
    private final SeatService seatService; //
    private final AppProperties appProperties; //

    @Transactional
    public BookingDetailsDto createBooking(CreateBookingRequest request) { //
        log.info("Bắt đầu tạo booking cho showtimeId: {}", request.getShowtimeId()); //

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId()) //
                .orElseThrow(() -> new IllegalArgumentException("Showtime không tồn tại: " + request.getShowtimeId()));
        
        if (!ShowtimeStatus.ACTIVE.equals(showtime.getStatus())) { //
            throw new IllegalArgumentException("Suất chiếu không còn hoạt động.");
        }
        if (showtime.getShowDateTime().isBefore(LocalDateTime.now())) { //
            throw new IllegalArgumentException("Suất chiếu đã diễn ra.");
        }

        Booking booking = new Booking(); //
        booking.setId(new ObjectId().toString()); //
        booking.setShowtimeId(showtime.getId()); //

        Booking.CustomerInfo customerInfo = new Booking.CustomerInfo( //
                request.getCustomerInfo().getFullName(), //
                request.getCustomerInfo().getPhone(), //
                request.getCustomerInfo().getEmail()); //
        booking.setCustomerInfo(customerInfo); //

        booking.setSeats(request.getSeats()); //
        booking.setBookingTime(LocalDateTime.now()); //
        
        // *** UPDATED ***
        booking.setPaymentStatus(PaymentStatusType.PENDING); 
        booking.setPaymentMethod(null); // Sẽ được set sau khi thanh toán thành công
        // *** END UPDATED ***
        
        booking.setCreatedAt(LocalDateTime.now()); //
        booking.setUpdatedAt(LocalDateTime.now()); //
        booking.setConfirmationCode(generateConfirmationCode()); //

        long totalTicketPrice = 0; //
        if (request.getTicketTypes() != null && !request.getTicketTypes().isEmpty()) { //
            List<Booking.TicketType> ticketTypes = request.getTicketTypes().stream().map(ttReq -> { //
                Booking.TicketType ticketType = new Booking.TicketType(); //
                ticketType.setType(ttReq.getType()); //
                ticketType.setQuantity(ttReq.getQuantity()); //
                ticketType.setPricePerTicket(ttReq.getPricePerTicket()); //
                ticketType.setSubtotal(ttReq.getQuantity() * ttReq.getPricePerTicket()); //
                return ticketType; //
            }).collect(Collectors.toList());
            booking.setTicketTypes(ticketTypes); //
            totalTicketPrice = ticketTypes.stream().mapToLong(Booking.TicketType::getSubtotal).sum(); //
        }
        
        long totalConcessionPrice = 0; //
        if (request.getConcessions() != null && !request.getConcessions().isEmpty()) { //
            // *** UPDATED ***
            List<Booking.ConcessionItem> bookingConcessions = request.getConcessions().stream().map(cReq -> {
                Concession concessionModel = concessionRepository.findById(cReq.getItemId()) //
                        .orElseThrow(() -> new IllegalArgumentException("Concession không tồn tại: " + cReq.getItemId()));
                if (!concessionModel.getAvailability()) { //
                     throw new IllegalArgumentException("Concession " + concessionModel.getName() + " không có sẵn.");
                }
                if (concessionModel.getCinemaIds() != null && !concessionModel.getCinemaIds().isEmpty() && !concessionModel.getCinemaIds().contains(showtime.getCinemaId())) { //
                    throw new IllegalArgumentException("Concession " + concessionModel.getName() + " không áp dụng cho rạp này.");
                }

                Booking.ConcessionItem bookingConcession = new Booking.ConcessionItem();
                bookingConcession.setItemId(cReq.getItemId()); //
                bookingConcession.setName(concessionModel.getName()); //
                bookingConcession.setQuantity(cReq.getQuantity()); //
                bookingConcession.setPrice(concessionModel.getPrice()); //
                return bookingConcession;
            }).collect(Collectors.toList());
            // *** END UPDATED ***
            booking.setConcessions(bookingConcessions); //
            totalConcessionPrice = bookingConcessions.stream().mapToLong(bc -> bc.getPrice() * bc.getQuantity()).sum(); //
        }

        booking.setTotalPrice(totalTicketPrice + totalConcessionPrice); //
        Booking savedBooking = bookingRepository.save(booking); //
        log.info("Đã tạo booking thành công với ID: {} và mã xác nhận: {}", savedBooking.getId(), savedBooking.getConfirmationCode()); //

// Kiểm tra và giữ ghế trước khi tạo booking
boolean seatsHeld = seatService.holdSeats(showtime.getId(), request.getSeats(), request.getCustomerInfo().getPhone());
if (!seatsHeld) {
    throw new IllegalStateException("Không thể giữ ghế đã chọn. Ghế có thể đã được đặt hoặc đang được giữ bởi người khác.");
}

        log.info("Đã tạo booking thành công với ID: {} và mã xác nhận: {}", savedBooking.getId(), savedBooking.getConfirmationCode());

        boolean seatsConfirmed = seatService.confirmSeatBooking(showtime.getId(), request.getSeats(), savedBooking.getId());
        if (!seatsConfirmed) {
            log.error("Không thể xác nhận ghế cho booking {}. Rolling back.", savedBooking.getId());
            // Rollback: release seats và delete booking
            seatService.releaseSeats(showtime.getId(), request.getSeats());
            bookingRepository.delete(savedBooking);
            throw new IllegalStateException("Lỗi hệ thống: Không thể xác nhận ghế đã chọn. Vui lòng thử lại.");
        }
        log.info("Đã xác nhận ghế cho booking: {}", savedBooking.getId()); //
        return getBookingDetailsDto(savedBooking); //
    }

    @Transactional
    public BookingDetailsDto confirmBookingPayment(String bookingId, PaymentMethodType paymentMethod, String paymentTransactionId) { //
        log.info("Xác nhận thanh toán cho booking ID: {}, Phương thức: {}, Tham chiếu TT: {}", bookingId, paymentMethod, paymentTransactionId); //
        Booking booking = bookingRepository.findById(bookingId) //
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại: " + bookingId));

        if (booking.getPaymentStatus() == PaymentStatusType.COMPLETED) { //
            log.warn("Booking {} đã được xác nhận thanh toán trước đó.", bookingId); //
            return getBookingDetailsDto(booking); //
        }
        
        // *** UPDATED ***
        booking.setPaymentStatus(PaymentStatusType.COMPLETED); 
        booking.setPaymentMethod(paymentMethod); 
        // *** END UPDATED ***
        booking.setPaymentReference(paymentTransactionId); //
        booking.setUpdatedAt(LocalDateTime.now()); //

        Booking updatedBooking = bookingRepository.save(booking); //
        log.info("Đã xác nhận thanh toán thành công cho booking: {}", updatedBooking.getId()); //
        return getBookingDetailsDto(updatedBooking); //
    }
    
    public Optional<BookingAggregatedDetailsDto> getBookingDetailsByConfirmationCode(String confirmationCode) {
        log.debug("Tra cứu booking chi tiết bằng mã xác nhận: {}", confirmationCode);
        // Tìm booking gốc trước
        Optional<Booking> bookingOpt = bookingRepository.findByConfirmationCode(confirmationCode);
        if (bookingOpt.isPresent()) {
            // Sau đó dùng ID của booking gốc để lấy thông tin đã aggregate
            return bookingRepository.findBookingWithDetailsById(bookingOpt.get().getId());
        }
        return Optional.empty();
    }

    public Optional<BookingAggregatedDetailsDto> lookupBookingDetails(String confirmationCode, String phone) {
        log.debug("Tra cứu booking chi tiết bằng mã xác nhận: {} và SĐT: {}", confirmationCode, phone);
        Optional<Booking> bookingOpt = bookingRepository.findByConfirmationCodeAndCustomerInfo_Phone(confirmationCode, phone);
        if (bookingOpt.isPresent()) {
            return bookingRepository.findBookingWithDetailsById(bookingOpt.get().getId());
        }
        return Optional.empty();
    }
    
    private String generateConfirmationCode() { //
        String prefix = appProperties.getBooking().getConfirmationCode().getPrefix(); //
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(); //
        return prefix + randomPart; //
    }

    private BookingDetailsDto getBookingDetailsDto(Booking booking) { //
        Showtime showtime = showtimeRepository.findById(booking.getShowtimeId()).orElse(null);
        Movie movie = null;
        Cinema cinema = null;
        Room room = null;
        LocalDateTime showDateTime = null;

        if (showtime != null) {
            movie = movieRepository.findById(showtime.getMovieId()).orElse(null);
            cinema = cinemaRepository.findById(showtime.getCinemaId()).orElse(null);
            room = roomRepository.findById(showtime.getRoomId()).orElse(null);
            showDateTime = showtime.getShowDateTime();
        }

        return BookingDetailsDto.fromBooking( //
            booking,
            movie != null ? movie.getTitle() : "N/A",
            cinema != null ? cinema.getName() : "N/A",
            room != null ? room.getName() : "N/A",
            showDateTime
        );
    }
}