package com.cinema.service;

import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.enums.ShowtimeStatus;
// Thêm import cho DTO mới
import com.cinema.dto.response.ShowtimeSummaryDto;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Cần cho mapping

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
        // Chuyển đổi String status từ model sang enum ShowtimeStatus cho DTO
        // Model Showtime có getStatusEnum() để làm việc này
        dto.setStatus(showtime.getStatusEnum()); // Sử dụng getStatusEnum() từ model Showtime
        return dto;
    }

    /**
     * Lấy danh sách suất chiếu có filter (trả về DTO tóm tắt).
     */
    public List<ShowtimeSummaryDto> getShowtimes(String movieId, String cinemaId, LocalDate date, String status) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : null;
        
        ShowtimeStatus showtimeStatusEnum; // Đổi tên để rõ ràng là Enum
        if (status != null && !status.trim().isEmpty()) { // Kiểm tra thêm trim() và isEmpty()
            try {
                showtimeStatusEnum = ShowtimeStatus.fromValue(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}, using ACTIVE as default", status);
                showtimeStatusEnum = ShowtimeStatus.ACTIVE;
            }
        } else {
            showtimeStatusEnum = ShowtimeStatus.ACTIVE; // Mặc định là ACTIVE nếu không có status hoặc status rỗng
        }

        log.debug("Fetching showtimes with movieId: {}, cinemaId: {}, date: {}, statusEnum: {}",
                  movieId, cinemaId, date, showtimeStatusEnum);

        List<Showtime> showtimes; // Biến tạm để lưu kết quả từ repository

        if (movieId != null && cinemaId != null && date != null) {
            showtimes = showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, startOfDay, endOfDay, showtimeStatusEnum);
        } else if (movieId != null && date != null) {
            showtimes = showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                    movieId, startOfDay, endOfDay, showtimeStatusEnum);
        } else if (cinemaId != null && date != null) {
            showtimes = showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                    cinemaId, startOfDay, endOfDay, showtimeStatusEnum);
        } else if (date != null) {
            showtimes = showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, showtimeStatusEnum);
        } else if (movieId != null && cinemaId != null) {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now().plusYears(1);
            showtimes = showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, start, end, showtimeStatusEnum);
        } else if (movieId != null) {
            showtimes = showtimeRepository.findByMovieIdAndStatus(movieId, showtimeStatusEnum);
        } else if (cinemaId != null) {
            showtimes = showtimeRepository.findByCinemaIdAndStatus(cinemaId, showtimeStatusEnum);
        } else {
            // Nếu không có filter nào khác ngoài status, thì lấy theo status
            showtimes = showtimeRepository.findByStatus(showtimeStatusEnum);
        }
        
        // Chuyển đổi List<Showtime> sang List<ShowtimeSummaryDto>
        return showtimes.stream()
                        .map(this::convertToShowtimeSummaryDto)
                        .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một suất chiếu (vẫn trả về Showtime đầy đủ).
     * Phương thức này không thay đổi vì client cần seatStatus đầy đủ.
     */
    public Optional<Showtime> getShowtimeById(String id) {
        log.debug("Fetching showtime by id: {}", id);
        // Chỉ lấy suất chiếu ACTIVE khi xem chi tiết, trừ khi có yêu cầu khác
        return showtimeRepository.findByIdAndStatus(id, ShowtimeStatus.ACTIVE);
    }
}