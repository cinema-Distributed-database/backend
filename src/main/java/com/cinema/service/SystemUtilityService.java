package com.cinema.service;

import com.cinema.repository.CinemaRepository;
import com.cinema.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
        // Có thể kiểm tra kết nối DB, các service khác ở đây
        log.debug("Kiểm tra health status");
        try {
            // Thử một query đơn giản để kiểm tra kết nối DB
            cinemaRepository.count(); 
            movieRepository.count();
            return Map.of("status", "UP", "database", "connected");
        } catch (Exception e) {
            log.error("Lỗi health check: {}", e.getMessage());
            return Map.of("status", "DOWN", "database", "disconnected", "error", e.getMessage());
        }
    }

    /**
     * Lấy danh sách các thành phố có rạp đang hoạt động.
     * @return List<String> tên các thành phố.
     */
    public List<String> getCitiesWithCinemas() {
        log.debug("Lấy danh sách thành phố có rạp");
        return cinemaRepository.findDistinctCitiesByStatus("active"); //
    }

    /**
     * Lấy danh sách tất cả các thể loại phim.
     * @return List<String> tên các thể loại.
     */
    public List<String> getAllMovieGenres() {
        log.debug("Lấy danh sách thể loại phim");
        return movieRepository.findDistinctGenres(); //
    }
}