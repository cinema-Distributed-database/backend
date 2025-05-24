package com.cinema.repository;

import com.cinema.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    
    List<Room> findByCinemaId(String cinemaId);
    
    List<Room> findByCinemaIdAndStatus(String cinemaId, String status);
    
    Optional<Room> findByCinemaIdAndRoomNumber(String cinemaId, String roomNumber);
    
    List<Room> findByStatus(String status);
    
    List<Room> findByCinemaIdAndType(String cinemaId, String type);
    
    long countByCinemaIdAndStatus(String cinemaId, String status);
}