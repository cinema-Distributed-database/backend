package com.cinema.service;

import com.cinema.dto.request.CreatePaymentRequestDto;
import com.cinema.dto.response.CreatePaymentResponseDto;
import com.cinema.model.Payment;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

public interface IVNPayService {
    CreatePaymentResponseDto createPaymentUrl(CreatePaymentRequestDto paymentRequest, HttpServletRequest httpServletRequest);
    Payment processVnpayCallback(Map<String, String> vnpParams);
    Payment processVnpayReturn(Map<String, String> vnpParams);
    Optional<Payment> getPaymentByBookingId(String bookingId);
    Optional<Payment> getPaymentByTransactionId(String transactionId); // vnp_TxnRef
    Optional<Payment> getPaymentById(String paymentId);
}