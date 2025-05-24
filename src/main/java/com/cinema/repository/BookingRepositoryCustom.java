package com.cinema.repository;

import com.cinema.dto.response.BookingAggregatedDetailsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface BookingRepositoryCustom {
    Optional<BookingAggregatedDetailsDto> findBookingWithDetailsById(String bookingId);
    Page<BookingAggregatedDetailsDto> findAllBookingsWithDetails(Pageable pageable); // Ví dụ cho danh sách
    // Thêm các phương thức tìm kiếm khác với aggregation nếu cần
}