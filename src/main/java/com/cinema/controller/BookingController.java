package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.dto.request.CreateBookingRequest;
import com.cinema.dto.request.LookupBookingRequest;
import com.cinema.dto.response.BookingAggregatedDetailsDto;
import com.cinema.dto.response.BookingDetailsDto;
import com.cinema.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cinema.enums.*;
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/bookings - Tạo booking mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingDetailsDto>> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        try {
            log.info("Request tạo booking mới cho showtimeId: {}", request.getShowtimeId());
            BookingDetailsDto bookingDetails = bookingService.createBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Booking đã được tạo thành công.", bookingDetails));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Lỗi khi tạo booking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tạo booking: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi hệ thống khi tạo booking."));
        }
    }

    /**
     * GET /api/bookings/{confirmationCode} - Tra cứu vé bằng mã xác nhận
     */
    @GetMapping("/{confirmationCode}")
    public ResponseEntity<ApiResponse<BookingAggregatedDetailsDto>> getBookingDetailsByConfirmationCode(@PathVariable String confirmationCode) {
        log.info("Request tra cứu booking chi tiết bằng mã xác nhận: {}", confirmationCode);
        return bookingService.getBookingDetailsByConfirmationCode(confirmationCode) // Gọi phương thức service mới
                .map(bookingDetails -> ResponseEntity.ok(ApiResponse.success(bookingDetails)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/bookings/lookup - Tra cứu vé (confirmationCode + phone)
     * Sử dụng POST để gửi body hoặc GET với RequestParams
     */
    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<BookingAggregatedDetailsDto>> lookupBookingDetails(@Valid @RequestBody LookupBookingRequest request) {
         log.info("Request tra cứu booking chi tiết với SĐT: {} và Email: {}", request.getPhone(), request.getEmail());
        return bookingService.lookupBookingDetails(request.getPhone(), request.getEmail()) // Gọi phương thức service mới
                .map(bookingDetails -> ResponseEntity.ok(ApiResponse.success(bookingDetails)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // GET version for /lookup (nếu muốn)
    // @GetMapping("/lookup")
    // public ResponseEntity<ApiResponse<BookingDetailsDto>> lookupBookingByParams(
    //         @RequestParam String confirmationCode,
    //         @RequestParam String phone) {
    //     log.info("Request tra cứu booking với mã: {} và SĐT: {}", confirmationCode, phone);
    //     return bookingService.lookupBooking(confirmationCode, phone)
    //             .map(bookingDetails -> ResponseEntity.ok(ApiResponse.success(bookingDetails)))
    //             .orElse(ResponseEntity.notFound().build());
    // }


    /**
     * PUT /api/bookings/{id}/confirm - Xác nhận sau thanh toán
     * Endpoint này thường không được gọi trực tiếp bởi client mà bởi hệ thống sau khi payment gateway callback.
     * Tuy nhiên, nếu bạn cần một API để admin xác nhận thủ công hoặc test, nó có thể hữu ích.
     * Trong luồng VNPay, BookingService.confirmBookingPayment sẽ được PaymentService gọi.
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingDetailsDto>> confirmBookingPayment(
            @PathVariable String id,
            @RequestParam String paymentMethod, // Ví dụ: "MANUAL_CONFIRMATION", "VNPay"
            @RequestParam String paymentReference) { // Ví dụ: "ADMIN_USER_XYZ", "VNPAY_TRANS_ID_123"
        try {
            log.info("Request xác nhận thanh toán cho bookingId: {}, method: {}, ref: {}", id, paymentMethod, paymentReference);
            PaymentMethodType paymentMethodType = PaymentMethodType.valueOf(paymentMethod.toUpperCase());
            BookingDetailsDto bookingDetails = bookingService.confirmBookingPayment(id, paymentMethodType, paymentReference);
            return ResponseEntity.ok(ApiResponse.success("Booking đã được xác nhận thanh toán.", bookingDetails));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Lỗi khi xác nhận thanh toán booking {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}