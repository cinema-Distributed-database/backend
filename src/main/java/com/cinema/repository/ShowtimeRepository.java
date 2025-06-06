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
    List<Showtime> findShowtimesWithHoldingSeats();

    List<Showtime> findByShowDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Showtime> findByCinemaIdAndShowDateTimeBetween(
            String cinemaId, LocalDateTime start, LocalDateTime end);

    // ===== FIX: Thay đổi từ ShowtimeStatus enum sang String để match với DB =====
    
    // Tìm kiếm với movieId (hỗ trợ cả ObjectId và String)
    @Query("{ $and: [ " +
           "{ $or: [ { 'movieId': ?0 }, { 'movieId': { $oid: ?0 } } ] }, " +
           "{ 'cinemaId': ?1 }, " +
           "{ 'showDateTime': { $gte: ?2, $lte: ?3 } }, " +
           "{ 'status': ?4 } " +
           "] }")
    List<Showtime> findByMovieIdAndCinemaIdAndShowDateTimeBetweenAndStatus(
            String movieId, String cinemaId, LocalDateTime startDateTime, LocalDateTime endDateTime, String status);

    @Query("{ $and: [ " +
           "{ $or: [ { 'movieId': ?0 }, { 'movieId': { $oid: ?0 } } ] }, " +
           "{ 'showDateTime': { $gte: ?1, $lte: ?2 } }, " +
           "{ 'status': ?3 } " +
           "] }")
    List<Showtime> findByMovieIdAndShowDateTimeBetweenAndStatus(
            String movieId, LocalDateTime startDateTime, LocalDateTime endDateTime, String status);
            
    List<Showtime> findByCinemaIdAndShowDateTimeBetweenAndStatus(
            String cinemaId, LocalDateTime startDateTime, LocalDateTime endDateTime, String status);

    List<Showtime> findByShowDateTimeBetweenAndStatus(
            LocalDateTime startDateTime, LocalDateTime endDateTime, String status);
            
    @Query("{ $and: [ " +
           "{ $or: [ { 'movieId': ?0 }, { 'movieId': { $oid: ?0 } } ] }, " +
           "{ 'status': ?1 } " +
           "] }")
    List<Showtime> findByMovieIdAndStatus(String movieId, String status);
    
    List<Showtime> findByCinemaIdAndStatus(String cinemaId, String status);

    Optional<Showtime> findByIdAndStatus(String id, String status);
    
    List<Showtime> findByStatus(String status);

    List<Showtime> findByHasHoldingSeatsTrue();
    
    // ===== Thêm methods không có status filter để test =====
    @Query("{ $or: [ { 'movieId': ?0 }, { 'movieId': { $oid: ?0 } } ] }")
    List<Showtime> findByMovieId(String movieId);
    
    List<Showtime> findByCinemaId(String cinemaId);
}