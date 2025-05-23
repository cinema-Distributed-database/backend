package com.cinema.repository;

import com.cinema.model.Showtime;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends MongoRepository<Showtime, String> {
    
    @Query("{ 'seatStatus': { $exists: true }, 'seatStatus': { $ne: {} } }")
    List<Showtime> findShowtimesWithHoldingSeats();
    
    List<Showtime> findByShowDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<Showtime> findByCinemaIdAndShowDateTimeBetween(
            String cinemaId, LocalDateTime start, LocalDateTime end);
}