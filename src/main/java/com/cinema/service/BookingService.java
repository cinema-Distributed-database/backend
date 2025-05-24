package com.cinema.service;

import com.cinema.config.AppProperties;
import com.cinema.dto.request.CreateBookingRequest;
import com.cinema.dto.response.BookingDetailsDto;
import com.cinema.model.*; // Import Cinema, Movie, Room, Showtime, Booking, Concession
import com.cinema.repository.*; // Import các repository cần thiết
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.bson.types.ObjectId;


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
    private final SeatService seatService; // Để xác nhận ghế
    private final AppProperties appProperties;

    /**
     * Tạo booking mới.
     */
    @Transactional
    public BookingDetailsDto createBooking(CreateBookingRequest request) {
        log.info("Bắt đầu tạo booking cho showtimeId: {}", request.getShowtimeId());

        // 1. Kiểm tra Showtime
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Showtime không tồn tại: " + request.getShowtimeId()));
        
        if (!"active".equalsIgnoreCase(showtime.getStatus())) {
            throw new IllegalArgumentException("Suất chiếu không còn hoạt động.");
        }
        if (showtime.getShowDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Suất chiếu đã diễn ra.");
        }

        // 2. Kiểm tra và xác nhận ghế (chuyển từ holding sang booked)
        // Giả định rằng các ghế này đã được `holdSeats` trước đó.
        // Nếu API cho phép đặt trực tiếp không qua bước hold, logic ở đây cần kiểm tra ghế available và chuyển sang booked.
        // Hiện tại, theo luồng API, client sẽ gọi /hold trước, sau đó mới /bookings.
        // Tuy nhiên, để an toàn, ta nên kiểm tra lại trạng thái ghế.
        
        // Bước này quan trọng: đảm bảo các ghế yêu cầu đang ở trạng thái "holding" (do client giữ)
        // và thuộc về khách hàng này (nếu có cơ chế xác thực người giữ ghế).
        // Vì API hiện tại không có context người dùng, ta chỉ kiểm tra chung.
        // Nếu booking này là kết quả của việc người dùng chọn ghế và bấm "Đặt vé" ngay,
        // thì SeatService.holdSeats nên được gọi ở đây hoặc trước đó với thông tin khách hàng.
        // Trong trường hợp này, giả sử ghế đã được giữ và giờ là lúc xác nhận.
        
        // Nếu muốn chặt chẽ hơn:
        // for (String seatId : request.getSeats()) {
        //    Showtime.SeatStatus status = showtime.getSeatStatus().get(seatId);
        //    if (status == null || !"holding".equals(status.getStatus())) {
        //        throw new IllegalStateException("Ghế " + seatId + " không ở trạng thái đang giữ hoặc đã được đặt.");
        //    }
        //    // Optional: if (status.getCustomerId() != null && !status.getCustomerId().equals(request.getCustomerInfo().getPhone())) {
        //    //    throw new SecurityException("Ghế " + seatId + " đang được giữ bởi người khác.");
        //    // }
        // }


        // 3. Tạo đối tượng Booking
        Booking booking = new Booking();
        booking.setId(new ObjectId().toString()); // Tạo ID mới cho booking
        booking.setShowtimeId(showtime.getId());

        Booking.CustomerInfo customerInfo = new Booking.CustomerInfo(
                request.getCustomerInfo().getFullName(),
                request.getCustomerInfo().getPhone(),
                request.getCustomerInfo().getEmail());
        booking.setCustomerInfo(customerInfo);

        booking.setSeats(request.getSeats());
        booking.setBookingTime(LocalDateTime.now());
        booking.setPaymentStatus("PENDING"); // Trạng thái chờ thanh toán
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        
        // Tạo mã xác nhận duy nhất
        booking.setConfirmationCode(generateConfirmationCode());

        // 4. Xử lý TicketTypes và tính tổng tiền vé
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
        
        // 5. Xử lý Concessions và tính tổng tiền concessions
        long totalConcessionPrice = 0;
        if (request.getConcessions() != null && !request.getConcessions().isEmpty()) {
            List<Booking.Concession> bookingConcessions = request.getConcessions().stream().map(cReq -> {
                Concession concessionModel = concessionRepository.findById(cReq.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Concession không tồn tại: " + cReq.getItemId()));
                if (!concessionModel.getAvailability()) {
                     throw new IllegalArgumentException("Concession " + concessionModel.getName() + " không có sẵn.");
                }
                // Kiểm tra xem concession có áp dụng cho rạp này không
                if (concessionModel.getCinemaIds() != null && !concessionModel.getCinemaIds().isEmpty() && !concessionModel.getCinemaIds().contains(showtime.getCinemaId())) {
                    throw new IllegalArgumentException("Concession " + concessionModel.getName() + " không áp dụng cho rạp này.");
                }

                Booking.Concession bookingConcession = new Booking.Concession();
                bookingConcession.setItemId(cReq.getItemId());
                bookingConcession.setName(concessionModel.getName()); // Lấy tên từ DB
                bookingConcession.setQuantity(cReq.getQuantity());
                bookingConcession.setPrice(concessionModel.getPrice()); // Lấy giá từ DB
                return bookingConcession;
            }).collect(Collectors.toList());
            booking.setConcessions(bookingConcessions);
            totalConcessionPrice = bookingConcessions.stream().mapToLong(bc -> bc.getPrice() * bc.getQuantity()).sum();
        }

        booking.setTotalPrice(totalTicketPrice + totalConcessionPrice);

        // 6. Lưu Booking
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Đã tạo booking thành công với ID: {} và mã xác nhận: {}", savedBooking.getId(), savedBooking.getConfirmationCode());

        // 7. Xác nhận các ghế đã được đặt trong Showtime (chuyển từ 'holding' sang 'booked')
        // Điều này phải xảy ra SAU KHI booking được lưu thành công.
        boolean seatsConfirmed = seatService.confirmSeatBooking(showtime.getId(), request.getSeats(), savedBooking.getId());
        if (!seatsConfirmed) {
            // Xử lý rollback hoặc đánh dấu booking là lỗi
            log.error("Không thể xác nhận ghế cho booking {}. Tiến hành rollback (logic phức tạp, cần cân nhắc).", savedBooking.getId());
            // bookingRepository.delete(savedBooking); // Cách đơn giản nhất, nhưng có thể mất dữ liệu nếu thanh toán đã xảy ra
            // Hoặc: savedBooking.setPaymentStatus("FAILED_SEAT_CONFIRMATION"); bookingRepository.save(savedBooking);
            throw new IllegalStateException("Lỗi hệ thống: Không thể xác nhận ghế đã chọn. Vui lòng thử lại.");
        }
        
        log.info("Đã xác nhận ghế cho booking: {}", savedBooking.getId());

        // 8. Trả về thông tin chi tiết booking (hoặc chỉ mã xác nhận và tổng tiền)
        return getBookingDetailsDto(savedBooking);
    }

    /**
     * Tra cứu vé bằng mã xác nhận.
     */
    public Optional<BookingDetailsDto> getBookingByConfirmationCode(String confirmationCode) {
        log.debug("Tra cứu booking bằng mã xác nhận: {}", confirmationCode);
        return bookingRepository.findByConfirmationCode(confirmationCode)
                                .map(this::getBookingDetailsDto);
    }

    /**
     * Tra cứu vé bằng mã xác nhận và số điện thoại.
     */
    public Optional<BookingDetailsDto> lookupBooking(String confirmationCode, String phone) {
        log.debug("Tra cứu booking bằng mã xác nhận: {} và SĐT: {}", confirmationCode, phone);
        return bookingRepository.findByConfirmationCodeAndCustomerInfo_Phone(confirmationCode, phone)
                                .map(this::getBookingDetailsDto);
    }
    
    /**
     * Xác nhận booking sau khi thanh toán thành công.
     * (Thường được gọi bởi PaymentService sau khi VNPay callback xác nhận thanh toán)
     */
    @Transactional
    public BookingDetailsDto confirmBookingPayment(String bookingId, String paymentMethod, String paymentReference) {
        log.info("Xác nhận thanh toán cho booking ID: {}, Phương thức: {}, Tham chiếu: {}", bookingId, paymentMethod, paymentReference);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại: " + bookingId));

        if ("COMPLETED".equals(booking.getPaymentStatus())) {
            log.warn("Booking {} đã được xác nhận thanh toán trước đó.", bookingId);
            return getBookingDetailsDto(booking); // Trả về thông tin hiện tại nếu đã hoàn tất
        }
        
        // Kiểm tra các điều kiện khác nếu cần, ví dụ: không cho confirm booking đã hủy

        booking.setPaymentStatus("COMPLETED"); // Hoặc "PAID"
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentReference(paymentReference); // Mã giao dịch của VNPay
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Đã xác nhận thanh toán thành công cho booking: {}", updatedBooking.getId());

        // Gửi email/SMS xác nhận cho khách hàng (logic này nên ở một service riêng)
        // sendBookingConfirmationNotification(updatedBooking);

        return getBookingDetailsDto(updatedBooking);
    }
    
    // Helper method to generate a unique confirmation code
    private String generateConfirmationCode() {
        String prefix = appProperties.getBooking().getConfirmationCode().getPrefix();
        // Tạo một chuỗi ngẫu nhiên đủ dài để khó đoán và duy nhất
        // Ví dụ: CINESTAR + 8 ký tự chữ và số
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + randomPart;
    }

    // Helper method to convert Booking to BookingDetailsDto
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