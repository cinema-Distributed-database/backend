package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Cinema;
import com.cinema.model.Room;
import com.cinema.service.CinemaService;
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
@RequestMapping("/api/cinemas")
@RequiredArgsConstructor
public class CinemaController {
    
    private final CinemaService cinemaService;
    
    /**
     * GET /api/cinemas - Lấy danh sách tất cả rạp
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Cinema>>> getAllCinemas(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Cinema> cinemas = cinemaService.getAllCinemas(pageable);
        return ResponseEntity.ok(ApiResponse.success(cinemas));
    }
    
    /**
     * GET /api/cinemas/{id} - Thông tin chi tiết rạp
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Cinema>> getCinemaById(@PathVariable String id) {
        return cinemaService.getCinemaById(id)
                .map(cinema -> ResponseEntity.ok(ApiResponse.success(cinema)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/cinemas/nearby - Tìm rạp gần
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Cinema>>> getNearbyCinemas(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius) {
        
        List<Cinema> cinemas = cinemaService.findNearbyCinemas(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success(cinemas));
    }
    
    /**
     * GET /api/cinemas/{id}/rooms - Danh sách phòng chiếu của rạp
     */
    @GetMapping("/{id}/rooms")
    public ResponseEntity<ApiResponse<List<Room>>> getCinemaRooms(@PathVariable String id) {
        List<Room> rooms = cinemaService.getRoomsByCinema(id);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
    
    /**
     * GET /api/cinemas/by-city/{city} - Rạp theo thành phố
     */
    @GetMapping("/by-city/{city}")
    public ResponseEntity<ApiResponse<Page<Cinema>>> getCinemasByCity(
            @PathVariable String city,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Cinema> cinemas = cinemaService.getCinemasByCity(city, pageable);
        return ResponseEntity.ok(ApiResponse.success(cinemas));
    }
    
    /**
     * GET /api/cinemas/search - Tìm kiếm rạp
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Cinema>>> searchCinemas(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city) {
        
        List<Cinema> cinemas;
        
        if (q != null && !q.trim().isEmpty()) {
            cinemas = cinemaService.searchCinemasWithFilters(q, city);
        } else if (city != null && !city.trim().isEmpty()) {
            cinemas = cinemaService.searchCinemasWithFilters(null, city);
        } else {
            cinemas = cinemaService.searchCinemasWithFilters(null, null);
        }
        
        return ResponseEntity.ok(ApiResponse.success(cinemas));
    }
    
    /**
     * GET /api/cinemas/cities - Danh sách thành phố có rạp
     */
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<String>>> getCitiesWithCinemas() {
        List<String> cities = cinemaService.getCitiesWithCinemas();
        return ResponseEntity.ok(ApiResponse.success(cities));
    }
}