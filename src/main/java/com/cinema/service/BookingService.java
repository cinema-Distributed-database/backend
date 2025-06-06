package com.cinema.service;

import com.cinema.config.AppProperties;
import com.cinema.dto.request.CreateBookingRequest;
import com.cinema.dto.response.BookingAggregatedDetailsDto;
import com.cinema.dto.response.BookingDetailsDto;
import com.cinema.enums.*;
import com.cinema.model.*;
import com.cinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final ConcessionRepository concessionRepository;
    private final SeatService seatService;
    private final AppProperties appProperties;

    @Transactional
    public BookingDetailsDto createBooking(CreateBookingRequest request) {
        log.info("Bắt đầu tạo booking cho showtimeId: {}", request.getShowtimeId());

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Showtime không tồn tại: " + request.getShowtimeId()));

        // ** CẬP NHẬT: So sánh trực tiếp với enum **
        if (!ShowtimeStatus.ACTIVE.equals(showtime.getStatus())) {
            log.warn("Suất chiếu {} không ở trạng thái ACTIVE. Trạng thái hiện tại: {}", request.getShowtimeId(), showtime.getStatus());
            throw new IllegalArgumentException("Suất chiếu không còn hoạt động hoặc trạng thái không hợp lệ.");
        }
        if (showtime.getShowDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Suất chiếu đã diễn ra.");
        }

        // Bước 1: Giữ ghế trước khi tạo booking
        log.info("Đang tiến hành giữ ghế cho showtimeId: {}, seats: {}", showtime.getId(), request.getSeats());
        seatService.holdSeats(showtime.getId(), request.getSeats(), request.getCustomerInfo().getPhone());
        log.info("Đã giữ ghế thành công.");

        Booking booking = new Booking();
        booking.setId(new ObjectId().toString());
        booking.setShowtimeId(showtime.getId());

        Booking.CustomerInfo customerInfo = new Booking.CustomerInfo(
                request.getCustomerInfo().getFullName(),
                request.getCustomerInfo().getPhone(),
                request.getCustomerInfo().getEmail());
        booking.setCustomerInfo(customerInfo);

        booking.setSeats(request.getSeats());
        booking.setBookingTime(LocalDateTime.now());
        booking.setPaymentStatus(PaymentStatusType.PENDING);
        booking.setPaymentMethod(null); // Sẽ được set sau khi thanh toán thành công
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setConfirmationCode(generateConfirmationCode());

        long totalTicketPrice = 0;
        if (request.getTicketTypes() != null && !request.getTicketTypes().isEmpty()) {
            List<Booking.TicketType> ticketTypes = request.getTicketTypes().stream().map(ttReq -> {
                Booking.TicketType ticketType = new Booking.TicketType();
                ticketType.setType(ttReq.getType());
                ticketType.setQuantity(ttReq.getQuantity());
                ticketType.setPricePerTicket(ttReq.getPricePerTicket());
                ticketType.setSubtotal(ttReq.getQuantity() * ttReq.getPricePerTicket());
                return ticketType;
            }).collect(Collectors.toList());
            booking.setTicketTypes(ticketTypes);
            totalTicketPrice = ticketTypes.stream().mapToLong(Booking.TicketType::getSubtotal).sum();
        }

        long totalConcessionPrice = 0;
        if (request.getConcessions() != null && !request.getConcessions().isEmpty()) {
            List<Booking.ConcessionItem> bookingConcessions = request.getConcessions().stream().map(cReq -> {
                Concession concessionModel = concessionRepository.findById(cReq.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Concession không tồn tại: " + cReq.getItemId()));
                if (!concessionModel.getAvailability()) {
                     throw new IllegalArgumentException("Concession " + concessionModel.getName() + " không có sẵn.");
                }
                if (concessionModel.getCinemaIds() != null && !concessionModel.getCinemaIds().isEmpty() && !concessionModel.getCinemaIds().contains(showtime.getCinemaId())) {
                    throw new IllegalArgumentException("Concession " + concessionModel.getName() + " không áp dụng cho rạp này.");
                }
                Booking.ConcessionItem bookingConcession = new Booking.ConcessionItem();
                bookingConcession.setItemId(cReq.getItemId());
                bookingConcession.setName(concessionModel.getName());
                bookingConcession.setQuantity(cReq.getQuantity());
                bookingConcession.setPrice(concessionModel.getPrice());
                return bookingConcession;
            }).collect(Collectors.toList());
            booking.setConcessions(bookingConcessions);
            totalConcessionPrice = bookingConcessions.stream().mapToLong(bc -> bc.getPrice() * bc.getQuantity()).sum();
        }

        booking.setTotalPrice(totalTicketPrice + totalConcessionPrice);

        // Bước 2: Lưu booking vào DB
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Đã tạo booking (chờ thanh toán) thành công với ID: {} và mã xác nhận: {}", savedBooking.getId(), savedBooking.getConfirmationCode());

        return getBookingDetailsDto(savedBooking);
    }

    /**
     * Phương thức này được gọi bởi VNPayService sau khi thanh toán thành công để hoàn tất booking.
     * Nó bao gồm việc cập nhật trạng thái thanh toán và xác nhận ghế.
     */
    @Transactional
    public BookingDetailsDto finalizeSuccessfulPayment(String bookingId, PaymentMethodType paymentMethod, String paymentTransactionId) {
        log.info("Hoàn tất booking sau thanh toán thành công cho ID: {}, Phương thức: {}, Tham chiếu TT: {}", bookingId, paymentMethod, paymentTransactionId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại: " + bookingId));

        if (booking.getPaymentStatus() == PaymentStatusType.COMPLETED) {
            log.warn("Booking {} đã được hoàn tất thanh toán trước đó.", bookingId);
            return getBookingDetailsDto(booking);
        }

        booking.setPaymentStatus(PaymentStatusType.COMPLETED);
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentReference(paymentTransactionId);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Đã cập nhật trạng thái thanh toán thành công cho booking: {}", updatedBooking.getId());

        // Bước 3: Xác nhận ghế (chuyển từ HOLDING sang BOOKED)
        // Nếu bước này thất bại, Exception sẽ được ném ra và transaction ở VNPayService sẽ rollback
        seatService.confirmSeatBooking(updatedBooking.getShowtimeId(), updatedBooking.getSeats(), updatedBooking.getId());
        log.info("Đã xác nhận (BOOKED) ghế thành công cho bookingId: {}", updatedBooking.getId());

        return getBookingDetailsDto(updatedBooking);
    }
    
    // Legacy method, can be removed or kept for manual confirmation
    @Transactional
    public BookingDetailsDto confirmBookingPayment(String bookingId, PaymentMethodType paymentMethod, String paymentTransactionId) {
        return finalizeSuccessfulPayment(bookingId, paymentMethod, paymentTransactionId);
    }


    public Optional<BookingAggregatedDetailsDto> getBookingDetailsByConfirmationCode(String confirmationCode) {
        log.debug("Tra cứu booking chi tiết bằng mã xác nhận: {}", confirmationCode);
        return bookingRepository.findByConfirmationCode(confirmationCode)
                .flatMap(booking -> bookingRepository.findBookingWithDetailsById(booking.getId()));
    }

    public Optional<BookingAggregatedDetailsDto> lookupBookingDetails(String phone, String email) {
        log.debug("Tra cứu booking chi tiết bằng SĐT: {} và Email: {}", phone, email);
        return bookingRepository.findByCustomerInfo_PhoneAndCustomerInfo_Email(phone, email)
                .flatMap(booking -> bookingRepository.findBookingWithDetailsById(booking.getId()));
    }

    private String generateConfirmationCode() {
        String prefix = appProperties.getBooking().getConfirmationCode().getPrefix();
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + randomPart;
    }

    private BookingDetailsDto getBookingDetailsDto(Booking booking) {
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

        return BookingDetailsDto.fromBooking(
                booking,
                movie != null ? movie.getTitle() : "N/A",
                cinema != null ? cinema.getName() : "N/A",
                room != null ? room.getName() : "N/A",
                showDateTime
        );
    }
}