package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Cinema;
import com.cinema.model.Movie;
import com.cinema.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * GET /api/search - Tìm kiếm tổng hợp
     * Query parameters: q (keyword), city (optional)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchGeneral(
            @RequestParam String q,
            @RequestParam(required = false) String city) {
        log.info("Request tìm kiếm tổng hợp: q='{}', city='{}'", q, city);
        Map<String, Object> results = searchService.searchGeneral(q, city);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/search/movies - Tìm kiếm phim
     * Query parameters: q (keyword), genre (optional), status (optional: now-showing, coming-soon)
     */
    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<Movie>>> searchMovies(
            @RequestParam String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status) {
        log.info("Request tìm kiếm phim: q='{}', genre='{}', status='{}'", q, genre, status);
        List<Movie> movies = searchService.searchMovies(q, genre, status);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    /**
     * GET /api/search/cinemas - Tìm kiếm rạp
     * Query parameters: q (keyword), city (optional)
     */
    @GetMapping("/cinemas")
    public ResponseEntity<ApiResponse<List<Cinema>>> searchCinemas(
            @RequestParam String q,
            @RequestParam(required = false) String city) {
        log.info("Request tìm kiếm rạp: q='{}', city='{}'", q, city);
        List<Cinema> cinemas = searchService.searchCinemas(q, city);
        return ResponseEntity.ok(ApiResponse.success(cinemas));
    }
}