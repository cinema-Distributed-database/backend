package com.cinema.repository;

import com.cinema.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String>, BookingRepositoryCustom { // Thêm kế thừa
    Optional<Booking> findByConfirmationCode(String confirmationCode);
    Optional<Booking> findByCustomerInfo_PhoneAndCustomerInfo_Email(String phone, String email);
    List<Booking> findByCustomerInfo_PhoneOrderByBookingTimeDesc(String phone);
    List<Booking> findByShowtimeId(String showtimeId);
}