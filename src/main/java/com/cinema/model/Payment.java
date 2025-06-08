package com.cinema.model;

import com.cinema.enums.PaymentMethodType;
import com.cinema.enums.PaymentStatusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private String bookingId; // Liên kết với Booking
    private String transactionId; // Mã giao dịch của VNPay (vnp_TxnRef)
    private String vnpTransactionNo; // Mã giao dịch của VNPay (vnp_TransactionNo)
    private Long amount; // Số tiền thanh toán (đơn vị: VND * 100)
    private String orderInfo; // Thông tin đơn hàng
    private String responseCode; // Mã phản hồi từ VNPay
    private PaymentMethodType paymentMethod;
    private PaymentStatusType status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt; // Thời gian thanh toán thành công
    private String bankCode; // Mã ngân hàng
    private String vnpSecureHash; // Chữ ký bảo mật
    private Map<String, String> paymentLog; // Lưu toàn bộ params từ VNPay callback để đối soát
    private String confirmationCode;
}