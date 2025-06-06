package com.cinema.repository;

import com.cinema.enums.ShowtimeStatus;
import com.cinema.model.Showtime;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends MongoRepository<Showtime, String>, ShowtimeRepositoryCustom{

    // --- CÁC PHƯƠNG THỨC ĐƯỢC CẬP NHẬT ĐỂ DÙNG ENUM ---

    /**
     * Tìm suất chiếu theo nhiều điều kiện, sử dụng ShowtimeStatus enum.
     * Spring Data sẽ tự động sử dụng converter đã đăng ký.
     */
    List<Showtime> findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
            String movieId, String cinemaId, LocalDateTime startDateTime, LocalDateTime endDateTime, ShowtimeStatus status);

    List<Showtime> findByMovieIdAndShowDateTimeBetweenAndStatus(
            String movieId, LocalDateTime startDateTime, LocalDateTime endDateTime, ShowtimeStatus status);

    List<Showtime> findByCinemaIdAndShowDateTimeBetweenAndStatus(
            String cinemaId, LocalDateTime startDateTime, LocalDateTime endDateTime, ShowtimeStatus status);

    List<Showtime> findByShowDateTimeBetweenAndStatus(
            LocalDateTime startDateTime, LocalDateTime endDateTime, ShowtimeStatus status);

    List<Showtime> findByMovieIdAndStatus(String movieId, ShowtimeStatus status);

    List<Showtime> findByCinemaIdAndStatus(String cinemaId, ShowtimeStatus status);

    Optional<Showtime> findByIdAndStatus(String id, ShowtimeStatus status);

    List<Showtime> findByStatus(ShowtimeStatus status);


    // --- CÁC PHƯƠNG THỨC KHÁC ---

    /**
     * Tìm các suất chiếu đang có ghế ở trạng thái tạm giữ (HOLDING).
     * Cờ này giúp tối ưu việc quét định kỳ để giải phóng ghế hết hạn.
     */
    List<Showtime> findByHasHoldingSeatsTrue();

    /**
     * Tìm suất chiếu trong một khoảng thời gian (không phân biệt trạng thái).
     */
    List<Showtime> findByShowDateTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Tìm suất chiếu theo ID phim (không phân biệt trạng thái).
     */
    List<Showtime> findByMovieId(String movieId);

    /**
     * Tìm suất chiếu theo ID rạp (không phân biệt trạng thái).
     */
    List<Showtime> findByCinemaId(String cinemaId);
}