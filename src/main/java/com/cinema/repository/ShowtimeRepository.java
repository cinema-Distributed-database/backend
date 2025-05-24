package com.cinema.repository;

import com.cinema.model.Showtime;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends MongoRepository<Showtime, String> {

    @Query("{ 'seatStatus': { $exists: true }, 'seatStatus': { $ne: {} } }")
    List<Showtime> findShowtimesWithHoldingSeats(); // Đã có

    List<Showtime> findByShowDateTimeBetween(LocalDateTime start, LocalDateTime end); // Đã có

    List<Showtime> findByCinemaIdAndShowDateTimeBetween(
            String cinemaId, LocalDateTime start, LocalDateTime end); // Đã có

    // Mới: Lấy suất chiếu theo phim ID, rạp ID, và một khoảng thời gian (ví dụ: trong một ngày)
    List<Showtime> findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
            String movieId, String cinemaId, LocalDateTime startDateTime, LocalDateTime endDateTime, String status);

    List<Showtime> findByMovieIdAndShowDateTimeBetweenAndStatus(
            String movieId, LocalDateTime startDateTime, LocalDateTime endDateTime, String status);
            
    List<Showtime> findByCinemaIdAndShowDateTimeBetweenAndStatus(
            String cinemaId, LocalDateTime startDateTime, LocalDateTime endDateTime, String status);

    List<Showtime> findByShowDateTimeBetweenAndStatus(
            LocalDateTime startDateTime, LocalDateTime endDateTime, String status);
            
    List<Showtime> findByMovieIdAndStatus(String movieId, String status);
    
    List<Showtime> findByCinemaIdAndStatus(String cinemaId, String status);

    Optional<Showtime> findByIdAndStatus(String id, String status);
    
    List<Showtime> findByStatus(String status);

}