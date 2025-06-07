package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Showtime;
import com.cinema.dto.response.ShowtimeSummaryDto;
import com.cinema.service.ShowtimeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    /**
     * GET /api/showtimes - Lấy lịch chiếu (có filter, trả về DTO tóm tắt)
     */
    // <<< THAY ĐỔI: Cập nhật các tham số @RequestParam
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeSummaryDto>>> getShowtimes(
            @RequestParam(required = false) String movieId,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) String city, // <<< THAY ĐỔI: Thêm city
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // <<< THAY ĐỔI: Đổi `date` thành `startDate`
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,   // <<< THAY ĐỔI: Thêm `endDate`
            @RequestParam(required = false) String status) {
        
        log.info("=== API REQUEST: GET /api/showtimes ===");
        log.info("movieId: {}, cinemaId: {}, city: {}, startDate: {}, endDate: {}, status: {}", 
                 movieId, cinemaId, city, startDate, endDate, status); // <<< THAY ĐỔI: Cập nhật log
        
        // <<< THAY ĐỔI: Truyền các tham số mới vào service
        List<ShowtimeSummaryDto> showtimeSummaries = showtimeService.getShowtimes(movieId, cinemaId, city, startDate, endDate, status);
        
        log.info("=== API RESPONSE: {} showtimes found ===", showtimeSummaries.size());
        return ResponseEntity.ok(ApiResponse.success(showtimeSummaries));
    }

    /**
     * GET /api/showtimes/{id} - Chi tiết suất chiếu
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Showtime>> getShowtimeById(@PathVariable String id) {
        log.info("=== API REQUEST: GET /api/showtimes/{} ===", id);
        
        return showtimeService.getShowtimeById(id)
                .map(showtime -> {
                    log.info("=== API RESPONSE: Showtime found ===");
                    return ResponseEntity.ok(ApiResponse.success(showtime));
                })
                .orElseGet(() -> {
                    log.info("=== API RESPONSE: Showtime not found ===");
                    return ResponseEntity.notFound().build();
                });
    }
    
    // ===== THÊM TEST ENDPOINTS =====
    
    /**
     * TEST: GET /api/showtimes/debug/all - Lấy tất cả showtimes
     */
    @GetMapping("/debug/all")
    public ResponseEntity<ApiResponse<List<Showtime>>> getAllShowtimes() {
        log.info("=== DEBUG API: GET /api/showtimes/debug/all ===");
        List<Showtime> all = showtimeService.getAllShowtimes();
        return ResponseEntity.ok(ApiResponse.success(all));
    }
    
    /**
     * TEST: GET /api/showtimes/debug/by-movie/{movieId} - Test tìm theo movieId
     */
    @GetMapping("/debug/by-movie/{movieId}")
    public ResponseEntity<ApiResponse<List<Showtime>>> getByMovieIdOnly(@PathVariable String movieId) {
        log.info("=== DEBUG API: GET /api/showtimes/debug/by-movie/{} ===", movieId);
        List<Showtime> result = showtimeService.getShowtimesByMovieIdOnly(movieId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}