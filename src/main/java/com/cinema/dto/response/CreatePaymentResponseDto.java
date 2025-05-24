package com.cinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResponseDto {
    private boolean success;
    private String message;
    private String paymentUrl; // URL thanh toán VNPay
    private String paymentId;  // ID của bản ghi Payment trong DB của bạn
}