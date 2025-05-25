package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Showtime;
// Thêm import cho DTO mới
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
     * Query parameters: movieId, cinemaId, date (yyyy-MM-dd), status
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeSummaryDto>>> getShowtimes( // Thay đổi kiểu trả về ở đây
            @RequestParam(required = false) String movieId,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        log.info("Request lấy lịch chiếu với filter (summary) - movieId: {}, cinemaId: {}, date: {}, status: {}",
                 movieId, cinemaId, date, status);
        List<ShowtimeSummaryDto> showtimeSummaries = showtimeService.getShowtimes(movieId, cinemaId, date, status); // Gọi phương thức đã cập nhật
        return ResponseEntity.ok(ApiResponse.success(showtimeSummaries));
    }

    /**
     * GET /api/showtimes/{id} - Chi tiết suất chiếu (vẫn trả về Showtime đầy đủ)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Showtime>> getShowtimeById(@PathVariable String id) {
        log.info("Request lấy chi tiết suất chiếu: {}", id);
        return showtimeService.getShowtimeById(id)
                .map(showtime -> ResponseEntity.ok(ApiResponse.success(showtime)))
                .orElse(ResponseEntity.notFound().build()); //Sử dụng .orElse(ResponseEntity.notFound().build()); để trả 404 nếu không tìm thấy
    }
}