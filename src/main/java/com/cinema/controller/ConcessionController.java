package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.model.Concession;
import com.cinema.service.ConcessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/concessions")
@RequiredArgsConstructor
public class ConcessionController {

    private final ConcessionService concessionService;

    /**
     * GET /api/concessions - Danh sách đồ ăn thức uống
     * Query param: category (tùy chọn)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Concession>>> getAllConcessions(
            @RequestParam(required = false) String category) {
        log.info("Request lấy danh sách đồ ăn thức uống, category: {}", category);
        List<Concession> concessions;
        if (category != null && !category.trim().isEmpty()) {
            concessions = concessionService.getConcessionsByCategory(category);
        } else {
            concessions = concessionService.getAllConcessions();
        }
        return ResponseEntity.ok(ApiResponse.success(concessions));
    }

    /**
     * GET /api/concessions/by-cinema/{cinemaId} - Đồ ăn theo rạp
     */
    @GetMapping("/by-cinema/{cinemaId}")
    public ResponseEntity<ApiResponse<List<Concession>>> getConcessionsByCinema(@PathVariable String cinemaId) {
        log.info("Request lấy đồ ăn thức uống theo rạp: {}", cinemaId);
        List<Concession> concessions = concessionService.getConcessionsByCinema(cinemaId);
        return ResponseEntity.ok(ApiResponse.success(concessions));
    }

    /**
     * GET /api/concessions/{id} - Chi tiết sản phẩm
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Concession>> getConcessionById(@PathVariable String id) {
        log.info("Request lấy chi tiết đồ ăn thức uống: {}", id);
        return concessionService.getConcessionById(id)
                .map(concession -> ResponseEntity.ok(ApiResponse.success(concession)))
                .orElse(ResponseEntity.notFound().build());
    }
}