package com.cinema.service;

import com.cinema.model.Movie;
import com.cinema.repository.MovieRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final MongoTemplate mongoTemplate;
    
    /**
     * Lấy tất cả phim hoặc lọc theo trạng thái
     */
    public Page<Movie> getAllMovies(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return movieRepository.findByStatusAndIsActiveTrue(status, pageable);
        }
        return movieRepository.findByIsActiveTrue(pageable);
    }
    
    /**
     * Lấy thông tin chi tiết phim
     */
    public Optional<Movie> getMovieById(String id) {
        return movieRepository.findById(id);
    }
    
    /**
     * Lấy phim đang chiếu
     */
    public Page<Movie> getNowShowingMovies(Pageable pageable) {
        return movieRepository.findByStatusAndIsActiveTrueOrderByReleaseDateDesc("now-showing", pageable);
    }
    
    /**
     * Lấy phim sắp chiếu
     */
    public Page<Movie> getComingSoonMovies(Pageable pageable) {
        return movieRepository.findByStatusAndIsActiveTrueOrderByReleaseDateAsc("coming-soon", pageable);
    }
    
    /**
     * Tìm kiếm phim theo từ khóa
     */
    public List<Movie> searchMovies(String keyword) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
        Query query = TextQuery.queryText(criteria)
                .sortByScore();
        query.addCriteria(Criteria.where("isActive").is(true));
        
        return mongoTemplate.find(query, Movie.class);
    }
    
    /**
     * Tìm kiếm phim theo thể loại
     */
    public Page<Movie> getMoviesByGenre(String genre, Pageable pageable) {
        return movieRepository.findByGenresContainingAndIsActiveTrue(genre, pageable);
    }
    
    /**
     * Tìm kiếm phim với bộ lọc
     */
    public List<Movie> searchMoviesWithFilters(String keyword, String genre, String status) {
        Query query = new Query();
        
        // Thêm điều kiện tìm kiếm text nếu có keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
            query = TextQuery.queryText(textCriteria).sortByScore();
        }
        
        // Thêm filter theo thể loại
        if (genre != null && !genre.trim().isEmpty()) {
            query.addCriteria(Criteria.where("genres").in(genre));
        }
        
        // Thêm filter theo trạng thái
        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        // Chỉ lấy phim đang active
        query.addCriteria(Criteria.where("isActive").is(true));
        
        return mongoTemplate.find(query, Movie.class);
    }
    
    /**
     * Lấy danh sách thể loại phim
     */
    public List<String> getAllGenres() {
        return movieRepository.findDistinctGenres();
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