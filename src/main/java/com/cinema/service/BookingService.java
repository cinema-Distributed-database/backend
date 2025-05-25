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

        if (showtime.getStatus() == null || !ShowtimeStatus.ACTIVE.equals(showtime.getStatusEnum())) { 
            log.warn("Suất chiếu {} không ở trạng thái ACTIVE. Trạng thái hiện tại: {}", request.getShowtimeId(), showtime.getStatus());
            throw new IllegalArgumentException("Suất chiếu không còn hoạt động hoặc trạng thái không hợp lệ.");
        }
        if (showtime.getShowDateTime().isBefore(LocalDateTime.now())) { //
            throw new IllegalArgumentException("Suất chiếu đã diễn ra.");
        }

        // --- BEGIN MODIFICATION ---
        // Bước 1: Giữ ghế trước
        log.info("Đang tiến hành giữ ghế cho showtimeId: {}, seats: {}, customerPhone: {}", showtime.getId(), request.getSeats(), request.getCustomerInfo().getPhone());
        // seatService.holdSeats sẽ ném exception nếu không thành công
        seatService.holdSeats(showtime.getId(), request.getSeats(), request.getCustomerInfo().getPhone());
        log.info("Đã giữ ghế thành công.");
        // --- END MODIFICATION ---

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

        booking.setPaymentStatus(PaymentStatusType.PENDING);
        booking.setPaymentMethod(null); // Sẽ được set sau khi thanh toán thành công

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
            booking.setConcessions(bookingConcessions); //
            totalConcessionPrice = bookingConcessions.stream().mapToLong(bc -> bc.getPrice() * bc.getQuantity()).sum(); //
        }

        booking.setTotalPrice(totalTicketPrice + totalConcessionPrice); //

        // Bước 2: Lưu booking sau khi đã giữ ghế thành công
        Booking savedBooking = bookingRepository.save(booking); //
        log.info("Đã tạo booking (chờ thanh toán) thành công với ID: {} và mã xác nhận: {}", savedBooking.getId(), savedBooking.getConfirmationCode()); //

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

        booking.setPaymentStatus(PaymentStatusType.COMPLETED);
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentReference(paymentTransactionId); //
        booking.setUpdatedAt(LocalDateTime.now()); //

        Booking updatedBooking = bookingRepository.save(booking); //
        log.info("Đã xác nhận thanh toán thành công cho booking: {}", updatedBooking.getId()); //

        // --- BEGIN MODIFICATION ---
        // Bước 3: Xác nhận ghế (chuyển từ HOLDING sang BOOKED) sau khi thanh toán thành công
        try {
            log.info("Đang tiến hành xác nhận (BOOKED) các ghế cho bookingId: {}", updatedBooking.getId());
            seatService.confirmSeatBooking(updatedBooking.getShowtimeId(), updatedBooking.getSeats(), updatedBooking.getId());
            log.info("Đã xác nhận (BOOKED) ghế thành công cho bookingId: {}", updatedBooking.getId());
        } catch (Exception e) {
            // Xử lý tình huống nghiêm trọng: Thanh toán đã thành công nhưng không thể xác nhận ghế.
            // Điều này không nên xảy ra nếu ghế đã được HELD đúng cách.
            // Cần có cơ chế cảnh báo cho admin hoặc xử lý thủ công.
            log.error("LỖI NGHIÊM TRỌNG: Thanh toán booking {} thành công nhưng không thể xác nhận ghế. Cần kiểm tra thủ công! Lỗi: {}", updatedBooking.getId(), e.getMessage(), e);
            // Không ném exception ra ngoài ở đây để tránh làm client hiểu lầm là thanh toán thất bại.
            // Trạng thái booking vẫn là COMPLETED.
        }
        // --- END MODIFICATION ---

        return getBookingDetailsDto(updatedBooking); //
    }

    public Optional<BookingAggregatedDetailsDto> getBookingDetailsByConfirmationCode(String confirmationCode) {
        log.debug("Tra cứu booking chi tiết bằng mã xác nhận: {}", confirmationCode);
        Optional<Booking> bookingOpt = bookingRepository.findByConfirmationCode(confirmationCode);
        if (bookingOpt.isPresent()) {
            return bookingRepository.findBookingWithDetailsById(bookingOpt.get().getId());
        }
        return Optional.empty();
    }

    public Optional<BookingAggregatedDetailsDto> lookupBookingDetails(String phone, String email) {
        log.debug("Tra cứu booking chi tiết bằng mã xác nhận: {} và SĐT: {}", phone, email);
        Optional<Booking> bookingOpt = bookingRepository.findByCustomerInfo_PhoneAndCustomerInfor_Email(phone, email);
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

    // Phương thức mới hoặc sửa đổi từ confirmBookingPayment
public BookingDetailsDto finalizeSuccessfulPayment(String bookingId, PaymentMethodType paymentMethod, String paymentTransactionId) {
    log.info("Hoàn tất booking sau thanh toán thành công cho ID: {}, Phương thức: {}, Tham chiếu TT: {}", bookingId, paymentMethod, paymentTransactionId);
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại: " + bookingId));

    if (booking.getPaymentStatus() == PaymentStatusType.COMPLETED) {
        log.warn("Booking {} đã được hoàn tất thanh toán trước đó.", bookingId);
        return getBookingDetailsDto(booking); // Trả về thông tin hiện tại
    }

    booking.setPaymentStatus(PaymentStatusType.COMPLETED);
    booking.setPaymentMethod(paymentMethod);
    booking.setPaymentReference(paymentTransactionId);
    booking.setUpdatedAt(LocalDateTime.now());

    Booking updatedBooking = bookingRepository.save(booking);
    log.info("Đã cập nhật trạng thái thanh toán thành công cho booking: {}", updatedBooking.getId());

    // Xác nhận ghế (chuyển từ HOLDING sang BOOKED)
    // Nếu bước này thất bại, Exception sẽ được ném ra và transaction ở VNPayService sẽ rollback
    seatService.confirmSeatBooking(updatedBooking.getShowtimeId(), updatedBooking.getSeats(), updatedBooking.getId());
    log.info("Đã xác nhận (BOOKED) ghế thành công cho bookingId: {}", updatedBooking.getId());

    return getBookingDetailsDto(updatedBooking);
}

}