package com.cinema.service;

import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.enums.ShowtimeStatus;
import com.cinema.dto.response.ShowtimeSummaryDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        dto.setStatus(showtime.getStatusEnum());
        return dto;
    }

    /**
     * Lấy danh sách suất chiếu có filter (trả về DTO tóm tắt).
     * FIX: Sử dụng String status thay vì enum để match với DB
     */
    public List<ShowtimeSummaryDto> getShowtimes(String movieId, String cinemaId, LocalDate date, String status) {
        log.info("=== DEBUG: getShowtimes called ===");
        log.info("movieId: {}", movieId);
        log.info("cinemaId: {}", cinemaId);
        log.info("date: {}", date);
        log.info("status: {}", status);
        
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : null;
        
        // FIX: Sử dụng trực tiếp String status thay vì convert sang enum
        String statusValue = (status != null && !status.trim().isEmpty()) ? status.trim() : "active";
        
        log.info("Using statusValue: {}", statusValue);
        log.info("Date range: {} to {}", startOfDay, endOfDay);

        List<Showtime> showtimes;

        // FIX: Gọi repository methods với String status
        if (movieId != null && cinemaId != null && date != null) {
            log.info("Case 1: movieId + cinemaId + date + status");
            showtimes = showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, startOfDay, endOfDay, statusValue);
        } else if (movieId != null && date != null) {
            log.info("Case 2: movieId + date + status");
            showtimes = showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                    movieId, startOfDay, endOfDay, statusValue);
        } else if (cinemaId != null && date != null) {
            log.info("Case 3: cinemaId + date + status");
            showtimes = showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                    cinemaId, startOfDay, endOfDay, statusValue);
        } else if (date != null) {
            log.info("Case 4: date + status");
            showtimes = showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, statusValue);
        } else if (movieId != null && cinemaId != null) {
            log.info("Case 5: movieId + cinemaId + status (wide date range)");
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now().plusYears(1);
            showtimes = showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, start, end, statusValue);
        } else if (movieId != null) {
            log.info("Case 6: movieId + status");
            showtimes = showtimeRepository.findByMovieIdAndStatus(movieId, statusValue);
        } else if (cinemaId != null) {
            log.info("Case 7: cinemaId + status");
            showtimes = showtimeRepository.findByCinemaIdAndStatus(cinemaId, statusValue);
        } else {
            log.info("Case 8: status only");
            showtimes = showtimeRepository.findByStatus(statusValue);
        }
        
        log.info("Found {} showtimes", showtimes.size());
        
        // Debug: Log first few results
        for (int i = 0; i < Math.min(3, showtimes.size()); i++) {
            Showtime s = showtimes.get(i);
            log.info("Showtime {}: id={}, movieId={}, status={}", i+1, s.getId(), s.getMovieId(), s.getStatus());
        }
        
        return showtimes.stream()
                        .map(this::convertToShowtimeSummaryDto)
                        .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một suất chiếu.
     * FIX: Sử dụng String status
     */
    public Optional<Showtime> getShowtimeById(String id) {
        log.info("=== DEBUG: getShowtimeById called ===");
        log.info("id: {}", id);
        
        // FIX: Sử dụng String thay vì enum
        Optional<Showtime> result = showtimeRepository.findByIdAndStatus(id, "active");
        
        log.info("Found showtime: {}", result.isPresent());
        if (result.isPresent()) {
            Showtime s = result.get();
            log.info("Showtime details: id={}, movieId={}, status={}", s.getId(), s.getMovieId(), s.getStatus());
        }
        
        return result;
    }
    
    // ===== THÊM METHODS ĐỂ TEST =====
    
    /**
     * Test method: Lấy tất cả showtimes không filter gì
     */
    public List<Showtime> getAllShowtimes() {
        log.info("=== DEBUG: getAllShowtimes called ===");
        List<Showtime> all = showtimeRepository.findAll();
        log.info("Total showtimes in DB: {}", all.size());
        
        for (int i = 0; i < Math.min(5, all.size()); i++) {
            Showtime s = all.get(i);
            log.info("Showtime {}: id={}, movieId={}, cinemaId={}, status={}", 
                    i+1, s.getId(), s.getMovieId(), s.getCinemaId(), s.getStatus());
        }
        
        return all;
    }
    
    /**
     * Test method: Tìm theo movieId không có status filter
     */
    public List<Showtime> getShowtimesByMovieIdOnly(String movieId) {
        log.info("=== DEBUG: getShowtimesByMovieIdOnly called ===");
        log.info("movieId: {}", movieId);
        
        List<Showtime> result = showtimeRepository.findByMovieId(movieId);
        log.info("Found {} showtimes for movieId: {}", result.size(), movieId);
        
        return result;
    }
}