package com.cinema.service;

import com.cinema.dto.response.ShowtimeSummaryDto;
import com.cinema.enums.ShowtimeStatus;
import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    // Helper method để chuyển đổi Showtime sang ShowtimeSummaryDto
    private ShowtimeSummaryDto convertToShowtimeSummaryDto(Showtime showtime) {
        if (showtime == null) {
            return null;
        }
        ShowtimeSummaryDto dto = new ShowtimeSummaryDto();
        dto.setId(showtime.getId());
        dto.setMovieId(showtime.getMovieId());
        dto.setCinemaId(showtime.getCinemaId());
        dto.setRoomId(showtime.getRoomId());
        dto.setShowDateTime(showtime.getShowDateTime());
        dto.setScreenType(showtime.getScreenType());
        dto.setTotalSeats(showtime.getTotalSeats());
        dto.setAvailableSeats(showtime.getAvailableSeats());
        dto.setStatus(showtime.getStatus());
        return dto;
    }

    /**
     * Lấy danh sách suất chiếu có filter (trả về DTO tóm tắt).
     */
    // <<< THAY ĐỔI: Cập nhật signature của phương thức
    public List<ShowtimeSummaryDto> getShowtimes(String movieIdStr, String cinemaIdStr, String city, LocalDate startDate, LocalDate endDate, String statusString) {
        log.info("Request lấy suất chiếu - movieId: {}, cinemaId: {}, city: {}, startDate: {}, endDate: {}, status: {}", 
                 movieIdStr, cinemaIdStr, city, startDate, endDate, statusString);

        ShowtimeStatus status = (statusString != null && !statusString.trim().isEmpty())
                ? ShowtimeStatus.fromValue(statusString.trim())
                : ShowtimeStatus.ACTIVE;

        // <<< THAY ĐỔI: Truyền các tham số mới vào phương thức repository
        List<Showtime> showtimes = showtimeRepository.findShowtimesByFlexibleFilters(movieIdStr, cinemaIdStr, city, startDate, endDate, status);

        if (showtimes.isEmpty()) {
            log.info("Không tìm thấy suất chiếu nào với các điều kiện đã cho.");
            return Collections.emptyList();
        }

        log.info("Đã tìm thấy {} suất chiếu.", showtimes.size());
        return showtimes.stream()
                .map(this::convertToShowtimeSummaryDto)
                .collect(Collectors.toList());
    }

    public Optional<Showtime> getShowtimeById(String id) {
        log.info("Request lấy chi tiết suất chiếu ID: {}", id);
        return showtimeRepository.findByIdAndStatus(id, ShowtimeStatus.ACTIVE);
    }

    // --- CÁC PHƯƠNG THỨC DEBUG/TEST ---
    public List<Showtime> getAllShowtimes() {
        log.info("DEBUG: Lấy tất cả suất chiếu trong DB.");
        return showtimeRepository.findAll();
    }

    public List<Showtime> getShowtimesByMovieIdOnly(String movieId) {
        log.info("DEBUG: Lấy suất chiếu theo movieId: {}", movieId);
        return showtimeRepository.findByMovieId(movieId);
    }
}