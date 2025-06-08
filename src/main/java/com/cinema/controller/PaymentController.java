package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.dto.request.CreatePaymentRequestDto;
import com.cinema.dto.response.CreatePaymentResponseDto;
import com.cinema.model.Booking; // *** THÊM IMPORT NÀY ***
import com.cinema.model.Payment;
import com.cinema.enums.*;
import com.cinema.repository.BookingRepository; // *** THÊM IMPORT NÀY ***
import com.cinema.service.IVNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IVNPayService vnPayService;
    private final BookingRepository bookingRepository; // *** INJECT BOOKING REPOSITORY ***
    
    @Value("${frontend.payment.success-url}")
    private String frontendSuccessUrl;

    @Value("${frontend.payment.failure-url}")
    private String frontendFailureUrl;

    @PostMapping("/vnpay/create")
    public ResponseEntity<ApiResponse<CreatePaymentResponseDto>> createVNPayPayment(
            @Valid @RequestBody CreatePaymentRequestDto paymentRequest,
            HttpServletRequest httpServletRequest) {
        log.info("Yêu cầu tạo URL thanh toán VNPay cho bookingId: {}", paymentRequest.getBookingId());
        CreatePaymentResponseDto paymentResponse = vnPayService.createPaymentUrl(paymentRequest, httpServletRequest);
        if (paymentResponse.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán thành công.", paymentResponse));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(paymentResponse.getMessage()));
        }
    }

    // Endpoint này là IPN, KHÔNG SỬA ĐỔI NỘI DUNG TRẢ VỀ
    @GetMapping("/vnpay/callback")
    public ResponseEntity<String> vnpayIPNCallback(@RequestParam Map<String, String> vnpParams) {
        log.info("VNPay IPN callback. Params: {}", vnpParams);
        try {
            Payment paymentResult = vnPayService.processVnpayCallback(vnpParams);
            if ("00".equals(paymentResult.getResponseCode())) {
                log.info("VNPay IPN xử lý thành công cho TxnRef: {}", paymentResult.getTransactionId());
                return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
            } else {
                log.warn("VNPay IPN báo lỗi/thất bại cho TxnRef: {}. ResponseCode: {}", paymentResult.getTransactionId(), paymentResult.getResponseCode());
                return ResponseEntity.ok("{\"RspCode\":\"" + paymentResult.getResponseCode() + "\",\"Message\":\"Transaction " + paymentResult.getStatus().name() + "\"}");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi xử lý VNPay IPN (tham số không hợp lệ): {}", e.getMessage());
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum or Transaction Data\"}");
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xử lý VNPay IPN: ", e);
            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"System Error\"}");
        }
    }
    
    // *** CẬP NHẬT PHƯƠNG THỨC NÀY ***
    @GetMapping("/vnpay/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> vnpParams) {
        log.info("VNPay return URL. Params: {}", vnpParams);
        String redirectUrlStr;

        try {
            Payment paymentResult = vnPayService.processVnpayReturn(vnpParams);

            // Lấy confirmationCode từ bookingId trong paymentResult
            String confirmationCode = bookingRepository.findById(paymentResult.getBookingId())
                                        .map(Booking::getConfirmationCode)
                                        .orElse("NOT_FOUND"); // Giá trị mặc định nếu không tìm thấy

            String status = paymentResult.getStatus().name().toLowerCase();
            String responseCode = paymentResult.getResponseCode();
            String message;

            if (paymentResult.getStatus() == PaymentStatusType.COMPLETED) {
                 message = "Thanh toán thành công!";
                 redirectUrlStr = String.format("%s?confirmationCode=%s&status=%s&code=%s&message=%s",
                                               frontendSuccessUrl, confirmationCode, status, responseCode, URLEncoder.encode(message, StandardCharsets.UTF_8));
            } else {
                 message = "Thanh toán thất bại. Mã lỗi VNPay: " + responseCode;
                 redirectUrlStr = String.format("%s?confirmationCode=%s&status=%s&code=%s&message=%s",
                                               frontendFailureUrl, confirmationCode, status, responseCode, URLEncoder.encode(message, StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi xử lý VNPay return: {}", e.getMessage());
            redirectUrlStr = String.format("%s?error=%s&code=CLIENT_ERROR", 
                                           frontendFailureUrl, URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xử lý VNPay return: ", e);
            redirectUrlStr = String.format("%s?error=%s&code=SYSTEM_ERROR",
                                           frontendFailureUrl, URLEncoder.encode("Lỗi hệ thống, vui lòng thử lại hoặc liên hệ hỗ trợ.", StandardCharsets.UTF_8));
        }
        log.info("Redirecting client to: {}", redirectUrlStr);
        return new RedirectView(redirectUrlStr);
    }

    @GetMapping("/{paymentReference}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentStatus(@PathVariable String paymentReference) {
        log.info("Yêu cầu kiểm tra trạng thái thanh toán cho mã tham chiếu: {}", paymentReference);
        Optional<Payment> paymentOpt = vnPayService.getPaymentById(paymentReference);
        if (paymentOpt.isEmpty()) {
            paymentOpt = vnPayService.getPaymentByTransactionId(paymentReference);
        }
        
        return paymentOpt
                .map(payment -> ResponseEntity.ok(ApiResponse.success(payment)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                             .body(ApiResponse.error("Không tìm thấy thông tin thanh toán cho mã: " + paymentReference)));
    }
}