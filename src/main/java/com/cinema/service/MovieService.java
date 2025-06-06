package com.cinema.service;

import com.cinema.model.Movie;
import com.cinema.repository.MovieRepository; // Đảm bảo import này
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Thêm import này

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MongoTemplate mongoTemplate;

    // ... các phương thức khác không đổi ...
    public Page<Movie> getAllMovies(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return movieRepository.findByStatusAndIsActiveTrue(status, pageable);
        }
        return movieRepository.findByIsActiveTrue(pageable);
    }

    public Optional<Movie> getMovieById(String id) {
        return movieRepository.findById(id);
    }

    public Page<Movie> getNowShowingMovies(Pageable pageable) {
        return movieRepository.findByStatusAndIsActiveTrueOrderByReleaseDateDesc("now-showing", pageable);
    }

    public Page<Movie> getComingSoonMovies(Pageable pageable) {
        return movieRepository.findByStatusAndIsActiveTrueOrderByReleaseDateAsc("coming-soon", pageable);
    }

    public List<Movie> searchMovies(String keyword) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
        Query query = TextQuery.queryText(criteria)
                .sortByScore();
        query.addCriteria(Criteria.where("isActive").is(true));

        return mongoTemplate.find(query, Movie.class);
    }

    public Page<Movie> getMoviesByGenre(String genre, Pageable pageable) {
        return movieRepository.findByGenresContainingAndIsActiveTrue(genre, pageable);
    }

    // --- CẬP NHẬT PHƯƠNG THỨC NÀY ---
    public List<Movie> searchMoviesWithFilters(String keyword, String genre, String status, String country) {
        Query query = new Query();

        if (keyword != null && !keyword.trim().isEmpty()) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
            query = TextQuery.queryText(textCriteria).sortByScore();
        }

        if (genre != null && !genre.trim().isEmpty()) {
            query.addCriteria(Criteria.where("genres").in(genre));
        }

        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        if (country != null && !country.trim().isEmpty()) { // THÊM BỘ LỌC COUNTRY
            query.addCriteria(Criteria.where("country").is(country));
        }

        query.addCriteria(Criteria.where("isActive").is(true));

        return mongoTemplate.find(query, Movie.class);
    }


    /**
     * Lấy danh sách thể loại phim
     */
    public List<String> getAllGenres() {
        return movieRepository.findDistinctGenres().stream()
                .map(MovieRepository.GenreProjection::getGenre)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy danh sách quốc gia
     */
    public List<String> getAllCountries() { // <-- THÊM PHƯƠNG THỨC MỚI
        return movieRepository.findDistinctCountries().stream()
                .map(MovieRepository.CountryProjection::getCountry)
                .collect(Collectors.toList());
    }

    /**
     * Lấy phim mới nhất
     */
    public List<Movie> getLatestMovies(int limit) {
        return movieRepository.findTop10ByIsActiveTrueOrderByReleaseDateDesc()
                .stream()
                .limit(limit)
                .toList();
    }
}