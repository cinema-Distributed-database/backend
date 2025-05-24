package com.cinema.repository;

import com.cinema.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByTransactionId(String transactionId); // Quan trọng để tìm giao dịch khi VNPay callback
    Optional<Payment> findByBookingId(String bookingId); // Để tìm thanh toán theo booking
}