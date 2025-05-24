package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.dto.request.CreatePaymentRequestDto;
import com.cinema.dto.response.CreatePaymentResponseDto;
import com.cinema.model.Payment; // Model bạn đã tạo
import com.cinema.enums.*; // Import the payment status enum
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
    
    // URL trang kết quả thanh toán của Frontend (cấu hình trong application.properties)
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

    // VNPay IPN (Instant Payment Notification) URL - Server to Server
    @GetMapping("/vnpay/callback")
    public ResponseEntity<String> vnpayIPNCallback(@RequestParam Map<String, String> vnpParams) {
        log.info("VNPay IPN callback. Params: {}", vnpParams);
        try {
            Payment paymentResult = vnPayService.processVnpayCallback(vnpParams);
            // Theo tài liệu VNPay, IPN cần trả về mã để VNPay biết đã nhận được.
            if ("00".equals(paymentResult.getResponseCode())) { // Giao dịch thành công theo VNPay
                log.info("VNPay IPN xử lý thành công cho TxnRef: {}", paymentResult.getTransactionId());
                return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
            } else { // Giao dịch thất bại hoặc lỗi theo VNPay
                log.warn("VNPay IPN báo lỗi/thất bại cho TxnRef: {}. ResponseCode: {}", paymentResult.getTransactionId(), paymentResult.getResponseCode());
                // Trả về mã lỗi tương ứng nếu có, hoặc mã chung
                return ResponseEntity.ok("{\"RspCode\":\"" + paymentResult.getResponseCode() + "\",\"Message\":\"Transaction " + paymentResult.getStatus().name() + "\"}");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi xử lý VNPay IPN (tham số không hợp lệ): {}", e.getMessage());
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum or Transaction Data\"}"); // 97: Chữ ký không hợp lệ
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xử lý VNPay IPN: ", e);
            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"System Error\"}"); // 99: Lỗi không xác định
        }
    }
    
    // VNPay Return URL - Client Browser Redirect
    @GetMapping("/vnpay/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> vnpParams) {
        log.info("VNPay return URL. Params: {}", vnpParams);
        String redirectUrlStr;
        try {
            Payment paymentResult = vnPayService.processVnpayReturn(vnpParams);
            // Xây dựng URL redirect về frontend với các tham số cần thiết
            String status = paymentResult.getStatus().name().toLowerCase();
            String bookingId = paymentResult.getBookingId();
            String paymentId = paymentResult.getId();
            String transactionId = paymentResult.getTransactionId(); 
            String responseCode = paymentResult.getResponseCode();
            String message = "Giao dịch " + status; // Thông báo chung

            if (paymentResult.getStatus() == PaymentStatusType.COMPLETED) {
                 message = "Thanh toán thành công!";
                 redirectUrlStr = String.format("%s?bookingId=%s&paymentId=%s&status=%s&transactionId=%s&code=%s&message=%s",
                                               frontendSuccessUrl, bookingId, paymentId, status, transactionId, responseCode, URLEncoder.encode(message, StandardCharsets.UTF_8));
            } else {
                 message = "Thanh toán thất bại. Mã lỗi VNPay: " + responseCode;
                 redirectUrlStr = String.format("%s?bookingId=%s&paymentId=%s&status=%s&transactionId=%s&code=%s&message=%s",
                                               frontendFailureUrl, bookingId, paymentId, status, transactionId, responseCode, URLEncoder.encode(message, StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi xử lý VNPay return (tham số không hợp lệ): {}", e.getMessage());
            String bookingIdAttempt = vnpParams.getOrDefault("vnp_TxnRef", "unknown").split("_")[0]; // Cố gắng lấy bookingId từ vnp_TxnRef
            redirectUrlStr = String.format("%s?bookingId=%s&error=%s&code=97", 
                                           frontendFailureUrl, bookingIdAttempt, URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xử lý VNPay return: ", e);
             String bookingIdAttempt = vnpParams.getOrDefault("vnp_TxnRef", "unknown").split("_")[0];
            redirectUrlStr = String.format("%s?bookingId=%s&error=%s&code=99", 
                                           frontendFailureUrl, bookingIdAttempt, URLEncoder.encode("Lỗi hệ thống, vui lòng thử lại.", StandardCharsets.UTF_8));
        }
        log.info("Redirecting client to: {}", redirectUrlStr);
        return new RedirectView(redirectUrlStr);
    }

    // Endpoint để client kiểm tra trạng thái thanh toán (nếu cần)
    @GetMapping("/{paymentReference}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentStatus(@PathVariable String paymentReference) {
        log.info("Yêu cầu kiểm tra trạng thái thanh toán cho mã tham chiếu: {}", paymentReference);
        // Có thể tìm theo paymentId hoặc transactionId (vnp_TxnRef)
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