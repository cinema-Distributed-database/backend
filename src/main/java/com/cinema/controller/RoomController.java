package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Room;
import com.cinema.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * GET /api/rooms/{id} - Thông tin chi tiết phòng
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> getRoomById(@PathVariable String id) {
        log.info("Request lấy thông tin chi tiết phòng: {}", id);
        return roomService.getRoomById(id) //
                .map(room -> ResponseEntity.ok(ApiResponse.success(room)))
                .orElse(ResponseEntity.notFound().build());
    }

}