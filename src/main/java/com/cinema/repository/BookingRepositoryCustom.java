package com.cinema.repository;

import com.cinema.dto.response.BookingAggregatedDetailsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface BookingRepositoryCustom {
    Optional<BookingAggregatedDetailsDto> findBookingWithDetailsById(String bookingId);
    Page<BookingAggregatedDetailsDto> findAllBookingsWithDetails(Pageable pageable);
}