package com.cinema.controller;

import com.cinema.dto.ApiResponse;
import com.cinema.dto.request.ExtendHoldRequest;
import com.cinema.dto.request.HoldSeatRequest;
import com.cinema.dto.request.ReleaseSeatRequest;
import com.cinema.dto.response.SeatStatusDto;
import com.cinema.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * GET /api/seats/showtime/{showtimeId} - Trạng thái ghế theo suất chiếu
     */
    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<ApiResponse<SeatStatusDto>> getSeatStatusByShowtime(@PathVariable String showtimeId) {
        log.info("Request lấy trạng thái ghế cho showtimeId: {}", showtimeId);
        return seatService.getSeatStatusForShowtime(showtimeId)
                .map(seatStatus -> ResponseEntity.ok(ApiResponse.success(seatStatus)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/seats/hold - Giữ ghế tạm thời
     */
    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<String>> holdSeats(@Valid @RequestBody HoldSeatRequest request) {
        try {
            log.info("Request giữ ghế: showtimeId={}, seats={}, phone={}", 
                     request.getShowtimeId(), request.getSeatIds(), request.getCustomerPhone());
            boolean success = seatService.holdSeats(request.getShowtimeId(), request.getSeatIds(), request.getCustomerPhone());
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Ghế đã được giữ thành công.", null));
            } else {
                // Trường hợp này SeatService nên throw exception nếu không thành công do ghế không available
                return ResponseEntity.badRequest().body(ApiResponse.error("Không thể giữ ghế. Vui lòng kiểm tra lại."));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Lỗi khi giữ ghế: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DELETE /api/seats/release - Hủy giữ ghế
     */
    @DeleteMapping("/release") // Hoặc dùng POST nếu body phức tạp hơn hoặc không muốn dùng DELETE với body
    public ResponseEntity<ApiResponse<String>> releaseSeats(@Valid @RequestBody ReleaseSeatRequest request) {
        try {
            log.info("Request hủy giữ ghế: showtimeId={}, seats={}", request.getShowtimeId(), request.getSeatIds());
            boolean success = seatService.releaseSeats(request.getShowtimeId(), request.getSeatIds());
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Ghế đã được hủy giữ thành công.", null));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không thể hủy giữ ghế hoặc không có ghế nào đang được giữ."));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi khi hủy giữ ghế: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * POST /api/seats/extend-hold - Gia hạn giữ ghế
     */
    @PostMapping("/extend-hold")
    public ResponseEntity<ApiResponse<String>> extendHold(@Valid @RequestBody ExtendHoldRequest request) {
        try {
            log.info("Request gia hạn giữ ghế: showtimeId={}, seats={}", request.getShowtimeId(), request.getSeatIds());
            boolean success = seatService.extendSeatHold(request.getShowtimeId(), request.getSeatIds());
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Đã gia hạn thời gian giữ ghế thành công.", null));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không thể gia hạn giữ ghế. Ghế có thể đã hết hạn hoặc không được giữ."));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Lỗi khi gia hạn giữ ghế: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}