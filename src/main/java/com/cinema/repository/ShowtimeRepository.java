package com.cinema.repository;

import com.cinema.model.Showtime;
import com.cinema.enums.ShowtimeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends MongoRepository<Showtime, String> {

    @Query("{ 'seatStatus': { $exists: true }, 'seatStatus': { $ne: {} } }")
    List<Showtime> findShowtimesWithHoldingSeats();

    List<Showtime> findByShowDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Showtime> findByCinemaIdAndShowDateTimeBetween(
            String cinemaId, LocalDateTime start, LocalDateTime end);

    // Thay đổi từ String sang ShowtimeStatus
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

    List<Showtime> findByHasHoldingSeatsTrue();
}