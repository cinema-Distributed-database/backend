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
        // FIX: Thay đổi parameter từ ShowtimeStatus sang String để match với Controller
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : null;
        
        // Mặc định là "active" nếu không có status
        String finalStatusValue = (status != null) ? status : ShowtimeStatus.ACTIVE.getValue();

        log.debug("Fetching showtimes with movieId: {}, cinemaId: {}, date: {}, status: {}",
                  movieId, cinemaId, date, finalStatusValue);

        if (movieId != null && cinemaId != null && date != null) {
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, startOfDay, endOfDay, finalStatusValue);
        } else if (movieId != null && date != null) {
            return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                    movieId, startOfDay, endOfDay, finalStatusValue);
        } else if (cinemaId != null && date != null) {
            return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                    cinemaId, startOfDay, endOfDay, finalStatusValue);
        } else if (date != null) {
            return showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, finalStatusValue);
        } else if (movieId != null && cinemaId != null) {
            // Lấy trong khoảng thời gian rộng nếu không có date cụ thể
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now().plusYears(1);
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, start, end, finalStatusValue);
        } else if (movieId != null) {
            return showtimeRepository.findByMovieIdAndStatus(movieId, finalStatusValue);
        } else if (cinemaId != null) {
            return showtimeRepository.findByCinemaIdAndStatus(cinemaId, finalStatusValue);
        } else {
            return showtimeRepository.findByStatus(finalStatusValue);
        }
    }

    /**
     * Lấy chi tiết một suất chiếu.
     */
    public Optional<Showtime> getShowtimeById(String id) {
        log.debug("Fetching showtime by id: {}", id);
        return showtimeRepository.findByIdAndStatus(id, ShowtimeStatus.ACTIVE.getValue());
    }

    /**
     * Lấy lịch chiếu theo phim.
     */
    public List<Showtime> getShowtimesByMovie(String movieId, LocalDate date) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for movieId: {} from {} to {}", movieId, startOfDay, endOfDay);
        return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                movieId, startOfDay, endOfDay, ShowtimeStatus.ACTIVE.getValue());
    }

    /**
     * Lấy lịch chiếu theo rạp.
     */
    public List<Showtime> getShowtimesByCinema(String cinemaId, LocalDate date) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for cinemaId: {} from {} to {}", cinemaId, startOfDay, endOfDay);
        return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                cinemaId, startOfDay, endOfDay, ShowtimeStatus.ACTIVE.getValue());
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
                startOfDay, endOfDay, ShowtimeStatus.ACTIVE.getValue());
    }
}