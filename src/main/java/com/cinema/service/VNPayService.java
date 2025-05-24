package com.cinema.service;

import com.cinema.dto.request.CreatePaymentRequestDto;
import com.cinema.dto.response.CreatePaymentResponseDto;
import com.cinema.enums.PaymentMethodType;
import com.cinema.enums.PaymentStatusType;
import com.cinema.model.Booking;
import com.cinema.model.Payment; // Model bạn đã tạo
import com.cinema.repository.BookingRepository;
import com.cinema.repository.PaymentRepository; // Repository bạn đã tạo
// Đảm bảo bạn có Gson dependency nếu sử dụng: com.google.code.gson:gson
// import com.google.gson.Gson; 
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService implements IVNPayService {

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.default-return-url}") // Sẽ được dùng nếu client không gửi `returnUrl`
    private String vnpDefaultReturnUrl;

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService; 

    @Override
    @Transactional
    public CreatePaymentResponseDto createPaymentUrl(CreatePaymentRequestDto paymentRequest, HttpServletRequest httpServletRequest) {
        try {
            Booking booking = bookingRepository.findById(paymentRequest.getBookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại với ID: " + paymentRequest.getBookingId()));

            if (PaymentStatusType.COMPLETED.name().equals(booking.getPaymentStatus())) {
                 log.warn("Booking {} đã được thanh toán.", booking.getId());
                 return new CreatePaymentResponseDto(false, "Booking này đã được thanh toán.", null, null);
            }

            // Tạo một bản ghi Payment mới cho mỗi lần yêu cầu tạo URL, hoặc tìm và cập nhật bản ghi PENDING cũ (tùy logic)
            // Để đơn giản và theo dõi tốt hơn, tạo mới mỗi lần.
            Payment payment = new Payment();
            payment.setId(new ObjectId().toString());
            payment.setBookingId(booking.getId());
            payment.setAmount(booking.getTotalPrice() * 100L); // VNPay yêu cầu đơn vị là đồng * 100
            payment.setOrderInfo("Thanh toan cho booking " + booking.getConfirmationCode());
            payment.setStatus(PaymentStatusType.PENDING);
            payment.setPaymentMethod(PaymentMethodType.VNPAY); // Sử dụng Enum bạn đã tạo
            payment.setCreatedAt(LocalDateTime.now());
            
            // vnp_TxnRef nên là duy nhất cho mỗi giao dịch thanh toán.
            // Kết hợp ID của payment record với một số ngẫu nhiên để đảm bảo tính duy nhất cao.
            String vnpTxnRef = payment.getId() + "_" + getRandomNumber(6);
            payment.setTransactionId(vnpTxnRef);
            
            String vnpIpAddr = getIpAddress(httpServletRequest);
            String clientReturnUrl = paymentRequest.getReturnUrl();
            String finalReturnUrl = (clientReturnUrl != null && !clientReturnUrl.trim().isEmpty()) 
                                    ? clientReturnUrl 
                                    : vnpDefaultReturnUrl;

            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnpTmnCode);
            vnpParams.put("vnp_Amount", String.valueOf(payment.getAmount()));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", payment.getTransactionId());
            vnpParams.put("vnp_OrderInfo", payment.getOrderInfo());
            vnpParams.put("vnp_OrderType", "other"); // Mã loại hàng hóa mặc định
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", finalReturnUrl); 
            vnpParams.put("vnp_IpAddr", vnpIpAddr);

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn thanh toán (ví dụ: 15 phút)
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Sắp xếp và tạo chuỗi hash
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnpSecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            
            payment.setVnpSecureHash(vnpSecureHash); // Lưu lại hash đã tạo để tham khảo (không dùng để verify callback)
            paymentRepository.save(payment); 

            String paymentFullUrl = vnpPayUrl + "?" + queryUrl;
            log.info("Tạo URL VNPay thành công cho Booking ID {}, Payment ID {}: {}", 
                     booking.getId(), payment.getId(), paymentFullUrl);
            return new CreatePaymentResponseDto(true, "Tạo URL thanh toán VNPay thành công.", paymentFullUrl, payment.getId());

        } catch (Exception e) {
            log.error("Lỗi khi tạo URL thanh toán VNPay cho bookingId {}: {}", 
                      paymentRequest.getBookingId(), e.getMessage(), e);
            return new CreatePaymentResponseDto(false, "Lỗi hệ thống khi tạo URL thanh toán. " + e.getMessage(), null, null);
        }
    }
    
    @Override
    @Transactional
    public Payment processVnpayCallback(Map<String, String> vnpParams) {
        log.info("Xử lý VNPay IPN Callback. Params: {}", vnpParams);
        return processVnpayResponse(vnpParams, "IPN_CALLBACK");
    }

    @Override
    @Transactional
    public Payment processVnpayReturn(Map<String, String> vnpParams) {
        log.info("Xử lý VNPay Client Return. Params: {}", vnpParams);
        return processVnpayResponse(vnpParams, "CLIENT_RETURN");
    }

    private Payment processVnpayResponse(Map<String, String> vnpParams, String logContext) {
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String receivedSecureHash = vnpParams.get("vnp_SecureHash");

        if (vnpTxnRef == null || vnpTxnRef.isEmpty()) {
            log.error("[{}] Phản hồi VNPay thiếu vnp_TxnRef. Params: {}", logContext, vnpParams);
            throw new IllegalArgumentException("Phản hồi VNPay không hợp lệ: Thiếu mã giao dịch (vnp_TxnRef).");
        }
        
        Payment payment = paymentRepository.findByTransactionId(vnpTxnRef)
                .orElseThrow(() -> {
                    log.error("[{}] Không tìm thấy Payment với vnp_TxnRef: {}. Params: {}", logContext, vnpTxnRef, vnpParams);
                    return new IllegalArgumentException("Giao dịch không tồn tại trong hệ thống (Mã: " + vnpTxnRef + ").");
                });
        
        // Xây dựng lại chuỗi hashData từ các tham số nhận được để xác minh chữ ký
        Map<String, String> fieldsToVerify = new HashMap<>(vnpParams);
        fieldsToVerify.remove("vnp_SecureHashType"); // Nếu có
        fieldsToVerify.remove("vnp_SecureHash");
        
        List<String> fieldNames = new ArrayList<>(fieldsToVerify.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fieldsToVerify.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII)); // VNPay dùng US_ASCII cho hash
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        String calculatedSecureHash = hmacSHA512(vnpHashSecret, hashData.toString());

        if (!calculatedSecureHash.equalsIgnoreCase(receivedSecureHash)) {
            log.warn("[{}] Chữ ký VNPay không hợp lệ cho TxnRef: {}. Received: {}, Calculated: {}. Params: {}", 
                     logContext, vnpTxnRef, receivedSecureHash, calculatedSecureHash, vnpParams);
            payment.setStatus(PaymentStatusType.FAILED);
            payment.setPaymentLog(new HashMap<>(vnpParams)); // Lưu lại toàn bộ params để debug
            payment.setResponseCode(vnpParams.getOrDefault("vnp_ResponseCode", "97")); // 97: Checksum invalid
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Chữ ký không hợp lệ từ VNPay. Giao dịch bị từ chối.");
        }

        // Chữ ký hợp lệ, tiếp tục xử lý
        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
        payment.setResponseCode(vnpResponseCode);
        payment.setPaymentLog(new HashMap<>(vnpParams)); // Lưu lại toàn bộ params
        payment.setVnpTransactionNo(vnpParams.get("vnp_TransactionNo")); // Mã giao dịch của VNPay
        payment.setBankCode(vnpParams.get("vnp_BankCode"));

        // Chỉ cập nhật trạng thái và booking nếu giao dịch đang PENDING
        // để tránh xử lý lặp lại nếu IPN và Return URL đến gần như đồng thời
        if (payment.getStatus() == PaymentStatusType.PENDING) {
        if ("00".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatusType.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            log.info("[{}] Thanh toán VNPay thành công cho PaymentID: {}, BookingID: {}, TxnRef: {}",
                     logContext, payment.getId(), payment.getBookingId(), vnpTxnRef);

            // Gọi BookingService để xác nhận booking và ghế TRONG CÙNG GIAO DỊCH
            try {
                bookingService.finalizeSuccessfulPayment(
                    payment.getBookingId(),
                    PaymentMethodType.VNPAY,
                    payment.getTransactionId()
                );
                log.info("Hoàn tất thành công payment và booking cho PaymentID: {}", payment.getId());
            } catch (Exception e) {
                // Nếu finalizeSuccessfulPayment (bao gồm confirmSeatBooking) thất bại
                log.error("LỖI NGHIÊM TRỌNG: Thanh toán VNPay thành công (TxnRef: {}) NHƯNG không thể hoàn tất booking (ID: {}). Giao dịch sẽ được rollback. Lỗi: {}", vnpTxnRef, payment.getBookingId(), e.getMessage(), e);
                // Ném một runtime exception để đảm bảo transaction rollback
                throw new RuntimeException("Không thể hoàn tất booking sau khi thanh toán thành công. TxnRef: " + vnpTxnRef + ", BookingID: " + payment.getBookingId(), e);
            }

        } else { // Các mã lỗi khác: Giao dịch thất bại
                payment.setStatus(PaymentStatusType.FAILED);
                log.info("[{}] Thanh toán VNPay thất bại cho PaymentID: {}, BookingID: {}, TxnRef: {}, ResponseCode: {}",
                         logContext, payment.getId(), payment.getBookingId(), vnpTxnRef, vnpResponseCode);
            }
        } else {
             log.warn("[{}] Payment {} đã ở trạng thái {} (không phải PENDING). Bỏ qua xử lý callback/return này. TxnRef: {}",
                      logContext, payment.getId(), payment.getStatus(), vnpTxnRef);
        }
        return paymentRepository.save(payment);
    }
    
    @Override
    public Optional<Payment> getPaymentByBookingId(String bookingId) {
        // Lấy payment mới nhất hoặc payment đã COMPLETED cho bookingId
        return paymentRepository.findByBookingId(bookingId)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatusType.COMPLETED)
                .findFirst()
                .or(() -> paymentRepository.findByBookingId(bookingId)
                                .stream()
                                .max(Comparator.comparing(Payment::getCreatedAt)));
    }

    @Override
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
    
    @Override
    public Optional<Payment> getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId);
    }

    // --- Helper Methods ---
    private String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("127.0.0.1")) {
                // Có thể lấy IP public của máy nếu đang test ở local và có cách lấy (ví dụ qua 1 service)
                // Tạm thời để 127.0.0.1 cho môi trường dev. VNPay có thể yêu cầu IP public thực.
                ipAddress = "127.0.0.1"; 
            }
        }
        return ipAddress;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(secretKeySpec);
            byte[] macData = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // Convert byte array to hex string
            StringBuilder sb = new StringBuilder(macData.length * 2);
            for(byte b: macData) {
               sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Lỗi khi tạo HMAC SHA512: " + e.getMessage(), e);
            throw new RuntimeException("Lỗi bảo mật khi tạo chữ ký thanh toán: " + e.getMessage(), e);
        }
    }
}