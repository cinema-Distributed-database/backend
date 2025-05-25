package com.cinema.service;

import com.cinema.repository.CinemaRepository;
import com.cinema.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUtilityService {

    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;

    /**
     * Kiểm tra sức khỏe của ứng dụng.
     * @return Map chứa trạng thái.
     */
    public Map<String, String> getHealthStatus() {
        log.debug("Kiểm tra health status");
        try {
            // Thử một query đơn giản để kiểm tra kết nối DB
            cinemaRepository.count(); 
            movieRepository.count();
            return Map.of("status", "UP", "database", "connected");
        } catch (Exception e) {
            log.error("Lỗi health check: {}", e.getMessage(), e); // Thêm 'e' vào log để có stack trace
            // Xử lý trường hợp getMessage() trả về null
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error during health check";
            return Map.of("status", "DOWN", "database", "disconnected", "error", errorMessage);
        }
    }
}