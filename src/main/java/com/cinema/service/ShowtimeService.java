package com.cinema.service;

import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.enums.ShowtimeStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    /**
     * Lấy danh sách suất chiếu có filter.
     */
    public List<Showtime> getShowtimes(String movieId, String cinemaId, LocalDate date, String status) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : null;
        
        // Chuyển đổi String sang ShowtimeStatus enum
        ShowtimeStatus showtimeStatus;
        if (status != null) {
            try {
                showtimeStatus = ShowtimeStatus.fromValue(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}, using ACTIVE as default", status);
                showtimeStatus = ShowtimeStatus.ACTIVE;
            }
        } else {
            showtimeStatus = ShowtimeStatus.ACTIVE;
        }

        log.debug("Fetching showtimes with movieId: {}, cinemaId: {}, date: {}, status: {}",
                  movieId, cinemaId, date, showtimeStatus);

        if (movieId != null && cinemaId != null && date != null) {
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, startOfDay, endOfDay, showtimeStatus);
        } else if (movieId != null && date != null) {
            return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                    movieId, startOfDay, endOfDay, showtimeStatus);
        } else if (cinemaId != null && date != null) {
            return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                    cinemaId, startOfDay, endOfDay, showtimeStatus);
        } else if (date != null) {
            return showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, showtimeStatus);
        } else if (movieId != null && cinemaId != null) {
            // Lấy trong khoảng thời gian rộng nếu không có date cụ thể
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now().plusYears(1);
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, start, end, showtimeStatus);
        } else if (movieId != null) {
            return showtimeRepository.findByMovieIdAndStatus(movieId, showtimeStatus);
        } else if (cinemaId != null) {
            return showtimeRepository.findByCinemaIdAndStatus(cinemaId, showtimeStatus);
        } else {
            return showtimeRepository.findByStatus(showtimeStatus);
        }
    }

    /**
     * Lấy chi tiết một suất chiếu.
     */
    public Optional<Showtime> getShowtimeById(String id) {
        log.debug("Fetching showtime by id: {}", id);
        return showtimeRepository.findByIdAndStatus(id, ShowtimeStatus.ACTIVE);
    }

    /**
     * Lấy lịch chiếu theo phim.
     */
    public List<Showtime> getShowtimesByMovie(String movieId, LocalDate date) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for movieId: {} from {} to {}", movieId, startOfDay, endOfDay);
        return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                movieId, startOfDay, endOfDay, ShowtimeStatus.ACTIVE);
    }

    /**
     * Lấy lịch chiếu theo rạp.
     */
    public List<Showtime> getShowtimesByCinema(String cinemaId, LocalDate date) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for cinemaId: {} from {} to {}", cinemaId, startOfDay, endOfDay);
        return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                cinemaId, startOfDay, endOfDay, ShowtimeStatus.ACTIVE);
    }
    
    /**
     * Lấy lịch chiếu theo ngày.
     */
    public List<Showtime> getShowtimesByDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for date: {} ({} to {})", date, startOfDay, endOfDay);
        return showtimeRepository.findByShowDateTimeBetweenAndStatus(
                startOfDay, endOfDay, ShowtimeStatus.ACTIVE);
    }
}