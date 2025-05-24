package com.cinema.service;

import com.cinema.model.Cinema;
import com.cinema.model.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final MovieService movieService; // Sử dụng lại các hàm tìm kiếm phim đã có
    private final CinemaService cinemaService; // Sử dụng lại các hàm tìm kiếm rạp đã có


    /**
     * Tìm kiếm tổng hợp (phim và rạp).
     * @param keyword Từ khóa tìm kiếm.
     * @param city (Tùy chọn) Lọc theo thành phố (chỉ áp dụng cho rạp).
     * @return Map chứa danh sách phim và rạp tìm được.
     */
    public Map<String, Object> searchGeneral(String keyword, String city) {
        log.debug("Tìm kiếm tổng hợp với từ khóa: '{}', thành phố: '{}'", keyword, city);
        Map<String, Object> results = new HashMap<>();

        // Tìm kiếm phim
        // Sử dụng hàm searchMoviesWithFilters từ MovieService
        // null cho genre và status để tìm theo keyword trên các trường text index của Movie
        List<Movie> movies = movieService.searchMoviesWithFilters(keyword, null, null);
        results.put("movies", movies);

        // Tìm kiếm rạp
        // Sử dụng hàm searchCinemasWithFilters từ CinemaService
        List<Cinema> cinemas = cinemaService.searchCinemasWithFilters(keyword, city);
        results.put("cinemas", cinemas);
        
        return results;
    }

    /**
     * Tìm kiếm phim (có thể được gọi trực tiếp bởi MovieController hoặc SearchController).
     * @param q Từ khóa.
     * @param genre Thể loại.
     * @param status Trạng thái phim (now-showing, coming-soon).
     * @return Danh sách phim.
     */
    public List<Movie> searchMovies(String q, String genre, String status) {
        log.debug("Tìm kiếm phim với từ khóa: '{}', thể loại: '{}', trạng thái: '{}'", q, genre, status);
        return movieService.searchMoviesWithFilters(q, genre, status); //
    }

    /**
     * Tìm kiếm rạp (có thể được gọi trực tiếp bởi CinemaController hoặc SearchController).
     * @param q Từ khóa.
     * @param city Thành phố.
     * @return Danh sách rạp.
     */
    public List<Cinema> searchCinemas(String q, String city) {
        log.debug("Tìm kiếm rạp với từ khóa: '{}', thành phố: '{}'", q, city);
         return cinemaService.searchCinemasWithFilters(q, city); //
    }
}