package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Movie;
import com.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    
    private final MovieService movieService;
    
    /**
     * GET /api/movies - Lấy danh sách phim
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Movie>>> getAllMovies(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Movie> movies = movieService.getAllMovies(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }
    
    /**
     * GET /api/movies/{id} - Lấy thông tin chi tiết phim
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> getMovieById(@PathVariable String id) {
        return movieService.getMovieById(id)
                .map(movie -> ResponseEntity.ok(ApiResponse.success(movie)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/movies/now-showing - Phim đang chiếu
     */
    @GetMapping("/now-showing")
    public ResponseEntity<ApiResponse<Page<Movie>>> getNowShowingMovies(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Movie> movies = movieService.getNowShowingMovies(pageable);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }
    
    /**
     * GET /api/movies/coming-soon - Phim sắp chiếu
     */
    @GetMapping("/coming-soon")
    public ResponseEntity<ApiResponse<Page<Movie>>> getComingSoonMovies(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Movie> movies = movieService.getComingSoonMovies(pageable);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }
    
    /**
     * GET /api/movies/search - Tìm kiếm phim
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Movie>>> searchMovies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status) {
        
        List<Movie> movies;
        
        if (q != null && !q.trim().isEmpty()) {
            movies = movieService.searchMoviesWithFilters(q, genre, status);
        } else if (genre != null && !genre.trim().isEmpty()) {
            movies = movieService.searchMoviesWithFilters(null, genre, status);
        } else {
            movies = movieService.searchMoviesWithFilters(null, null, status);
        }
        
        return ResponseEntity.ok(ApiResponse.success(movies));
    }
    
    /**
     * GET /api/movies/genres - Lấy danh sách thể loại
     */
    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<List<String>>> getAllGenres() {
        List<String> genres = movieService.getAllGenres();
        return ResponseEntity.ok(ApiResponse.success(genres));
    }
    
    /**
     * GET /api/movies/latest - Lấy phim mới nhất
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<Movie>>> getLatestMovies(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Movie> movies = movieService.getLatestMovies(limit);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }
}