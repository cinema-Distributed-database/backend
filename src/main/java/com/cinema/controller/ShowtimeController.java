package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Showtime;
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
     * GET /api/showtimes - Lấy lịch chiếu (có filter)
     * Query parameters: movieId, cinemaId, date (yyyy-MM-dd), status
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Showtime>>> getShowtimes(
            @RequestParam(required = false) String movieId,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        log.info("Request lấy lịch chiếu với filter - movieId: {}, cinemaId: {}, date: {}, status: {}",
                 movieId, cinemaId, date, status);
        List<Showtime> showtimes = showtimeService.getShowtimes(movieId, cinemaId, date, status);
        return ResponseEntity.ok(ApiResponse.success(showtimes));
    }

    /**
     * GET /api/showtimes/{id} - Chi tiết suất chiếu
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Showtime>> getShowtimeById(@PathVariable String id) {
        log.info("Request lấy chi tiết suất chiếu: {}", id);
        return showtimeService.getShowtimeById(id)
                .map(showtime -> ResponseEntity.ok(ApiResponse.success(showtime)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/showtimes/by-movie/{movieId} - Lịch chiếu theo phim
     * Query parameters: date (yyyy-MM-dd) (tùy chọn, mặc định là ngày hiện tại)
     */
    @GetMapping("/by-movie/{movieId}")
    public ResponseEntity<ApiResponse<List<Showtime>>> getShowtimesByMovie(
            @PathVariable String movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request lấy lịch chiếu theo phim: {}, ngày: {}", movieId, date);
        List<Showtime> showtimes = showtimeService.getShowtimesByMovie(movieId, date);
        return ResponseEntity.ok(ApiResponse.success(showtimes));
    }

    /**
     * GET /api/showtimes/by-cinema/{cinemaId} - Lịch chiếu theo rạp
     * Query parameters: date (yyyy-MM-dd) (tùy chọn, mặc định là ngày hiện tại)
     */
    @GetMapping("/by-cinema/{cinemaId}")
    public ResponseEntity<ApiResponse<List<Showtime>>> getShowtimesByCinema(
            @PathVariable String cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request lấy lịch chiếu theo rạp: {}, ngày: {}", cinemaId, date);
        List<Showtime> showtimes = showtimeService.getShowtimesByCinema(cinemaId, date);
        return ResponseEntity.ok(ApiResponse.success(showtimes));
    }

    /**
     * GET /api/showtimes/by-date - Lịch chiếu theo ngày
     * Query parameters: date (yyyy-MM-dd) (bắt buộc, hoặc mặc định là ngày hiện tại nếu không truyền)
     */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<Showtime>>> getShowtimesByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request lấy lịch chiếu theo ngày: {}", date);
        List<Showtime> showtimes = showtimeService.getShowtimesByDate(date);
        return ResponseEntity.ok(ApiResponse.success(showtimes));
    }
}