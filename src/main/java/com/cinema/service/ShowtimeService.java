package com.cinema.service;

import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
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
    private static final String ACTIVE_STATUS = "active"; // Hoặc một enum

    /**
     * Lấy danh sách suất chiếu có filter.
     */
    public List<Showtime> getShowtimes(String movieId, String cinemaId, LocalDate date, String status) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : null;
        String queryStatus = (status == null || status.trim().isEmpty()) ? ACTIVE_STATUS : status;

        log.debug("Fetching showtimes with movieId: {}, cinemaId: {}, date: {}, status: {}", 
                  movieId, cinemaId, date, queryStatus);

        if (movieId != null && cinemaId != null && date != null) {
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
                    movieId, cinemaId, startOfDay, endOfDay, queryStatus);
        } else if (movieId != null && date != null) {
            return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(
                    movieId, startOfDay, endOfDay, queryStatus);
        } else if (cinemaId != null && date != null) {
            return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(
                    cinemaId, startOfDay, endOfDay, queryStatus);
        } else if (date != null) {
            return showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, queryStatus);
        } else if (movieId != null && cinemaId != null) {
            // Lọc theo movieId và cinemaId không theo ngày cụ thể (có thể lấy tất cả suất chiếu active)
            // Điều này có thể trả về nhiều kết quả, cần cân nhắc hoặc thêm Pageable
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(movieId, cinemaId, LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1), queryStatus);
        } else if (movieId != null) {
            return showtimeRepository.findByMovieIdAndStatus(movieId, queryStatus);
        } else if (cinemaId != null) {
            return showtimeRepository.findByCinemaIdAndStatus(cinemaId, queryStatus);
        } else {
            return showtimeRepository.findByStatus(queryStatus);
        }
    }

    /**
     * Lấy chi tiết một suất chiếu.
     */
    public Optional<Showtime> getShowtimeById(String id) {
        log.debug("Fetching showtime by id: {}", id);
        return showtimeRepository.findByIdAndStatus(id, ACTIVE_STATUS);
        // Hoặc return showtimeRepository.findById(id) nếu muốn lấy cả suất chiếu không active
    }

    /**
     * Lấy lịch chiếu theo phim.
     */
    public List<Showtime> getShowtimesByMovie(String movieId, LocalDate date) {
         LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay() ;
         LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for movieId: {} from {} to {}", movieId, startOfDay, endOfDay);
        return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(movieId, startOfDay, endOfDay, ACTIVE_STATUS);
    }

    /**
     * Lấy lịch chiếu theo rạp.
     */
    public List<Showtime> getShowtimesByCinema(String cinemaId, LocalDate date) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for cinemaId: {} from {} to {}", cinemaId, startOfDay, endOfDay);
        return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(cinemaId, startOfDay, endOfDay, ACTIVE_STATUS);
    }
    
    /**
     * Lấy lịch chiếu theo ngày.
     */
    public List<Showtime> getShowtimesByDate(LocalDate date) {
        if (date == null) {
            // Mặc định lấy ngày hiện tại nếu không cung cấp
            date = LocalDate.now();
        }
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for date: {} ({} to {})", date, startOfDay, endOfDay);
        return showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, ACTIVE_STATUS);
    }
}