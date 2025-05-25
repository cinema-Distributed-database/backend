package com.cinema.service;

import com.cinema.model.Room;
import com.cinema.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomRepository roomRepository;
    
    /**
     * Lấy thông tin chi tiết phòng
     */
    public Optional<Room> getRoomById(String id) {
        return roomRepository.findById(id);
    }
    
    
    /**
     * Lấy danh sách phòng theo rạp
     */
    public List<Room> getRoomsByCinema(String cinemaId) {
        return roomRepository.findByCinemaIdAndStatus(cinemaId, "active");
    }
    
    /**
     * Lấy phòng theo rạp và số phòng
     */
    public Optional<Room> getRoomByCinemaAndRoomNumber(String cinemaId, String roomNumber) {
        return roomRepository.findByCinemaIdAndRoomNumber(cinemaId, roomNumber);
    }
    
    /**
     * Lấy danh sách phòng theo loại
     */
    public List<Room> getRoomsByType(String cinemaId, String type) {
        return roomRepository.findByCinemaIdAndType(cinemaId, type);
    }
    
    /**
     * Đếm số phòng hoạt động của rạp
     */
    public long countActiveRoomsByCinema(String cinemaId) {
        return roomRepository.countByCinemaIdAndStatus(cinemaId, "active");
    }
    
    /**
     * Kiểm tra phòng có khả dụng không
     */
    public boolean isRoomAvailable(String roomId) {
        return roomRepository.findById(roomId)
                .map(room -> "active".equals(room.getStatus()))
                .orElse(false);
    }
    
    /**
     * Lấy thông tin metadata của phòng (số ghế, loại ghế, etc.)
     */
    public Optional<Room.SeatMetadata> getRoomMetadata(String roomId) {
        return roomRepository.findById(roomId)
                .map(Room::getSeatMap)
                .map(Room.SeatMap::getMetadata);
    }
}