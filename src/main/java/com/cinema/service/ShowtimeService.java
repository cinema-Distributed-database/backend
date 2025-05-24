package com.cinema.service;

import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.enums.ShowtimeStatus; // THÊM IMPORT NẾU CHƯA CÓ

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
    // private static final String ACTIVE_STATUS = "active"; // Không cần nữa nếu dùng Enum trực tiếp

    /**
     * Lấy danh sách suất chiếu có filter.
     */
    public List<Showtime> getShowtimes(String movieId, String cinemaId, LocalDate date, String statusString) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : null;
        
        ShowtimeStatus queryStatusEnum = null;
        if (statusString != null && !statusString.trim().isEmpty()) {
            try {
                queryStatusEnum = ShowtimeStatus.fromValue(statusString);
            } catch (IllegalArgumentException e) {
                log.warn("Trạng thái suất chiếu không hợp lệ: {}. Mặc định sẽ không lọc theo trạng thái này.", statusString);
                // Hoặc có thể ném lỗi tùy theo yêu cầu nghiệp vụ
            }
        } else {
            queryStatusEnum = ShowtimeStatus.ACTIVE; // Mặc định là ACTIVE
        }
        
        final String finalStatusValue = (queryStatusEnum != null) ? queryStatusEnum.getValue() : null;


        log.debug("Fetching showtimes with movieId: {}, cinemaId: {}, date: {}, status: {} (enum: {})",
                  movieId, cinemaId, date, statusString, queryStatusEnum);

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
            return showtimeRepository.findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(movieId, cinemaId, LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1), finalStatusValue);
        } else if (movieId != null) {
            return showtimeRepository.findByMovieIdAndStatus(movieId, finalStatusValue);
        } else if (cinemaId != null) {
            return showtimeRepository.findByCinemaIdAndStatus(cinemaId, finalStatusValue);
        } else if (finalStatusValue != null) { // Chỉ lọc theo status nếu có
            return showtimeRepository.findByStatus(finalStatusValue);
        } else {
             // Nếu không có filter nào và status cũng không có, có thể trả về tất cả hoặc lỗi
             // Hiện tại đang trả về tất cả nếu statusString ban đầu là không hợp lệ và không có filter khác.
             // Để an toàn hơn, nếu finalStatusValue là null ở đây, nên có một logic rõ ràng.
             // Ví dụ, trả về tất cả các suất chiếu ACTIVE nếu không có status cụ thể.
             // Hoặc ném lỗi nếu không muốn trả về quá nhiều dữ liệu.
             // Nếu queryStatusEnum ban đầu là null do statusString không hợp lệ, finalStatusValue sẽ là null.
             // Nếu bạn muốn mặc định là ACTIVE khi statusString không hợp lệ:
             String effectiveStatus = (finalStatusValue != null) ? finalStatusValue : ShowtimeStatus.ACTIVE.getValue();
            return showtimeRepository.findByStatus(effectiveStatus);
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
         LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay() ;
         LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for movieId: {} from {} to {}", movieId, startOfDay, endOfDay);
        return showtimeRepository.findByMovieIdAndShowDateTimeBetweenAndStatus(movieId, startOfDay, endOfDay, ShowtimeStatus.ACTIVE.getValue());
    }

    /**
     * Lấy lịch chiếu theo rạp.
     */
    public List<Showtime> getShowtimesByCinema(String cinemaId, LocalDate date) {
        LocalDateTime startOfDay = date != null ? date.atStartOfDay() : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        log.debug("Fetching showtimes for cinemaId: {} from {} to {}", cinemaId, startOfDay, endOfDay);
        return showtimeRepository.findByCinemaIdAndShowDateTimeBetweenAndStatus(cinemaId, startOfDay, endOfDay, ShowtimeStatus.ACTIVE.getValue());
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
        return showtimeRepository.findByShowDateTimeBetweenAndStatus(startOfDay, endOfDay, ShowtimeStatus.ACTIVE.getValue());
    }
}