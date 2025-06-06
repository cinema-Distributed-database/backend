package com.cinema.repository;

import com.cinema.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends MongoRepository<Movie, String> {
    
    Page<Movie> findByIsActiveTrue(Pageable pageable);
    
    Page<Movie> findByStatusAndIsActiveTrue(String status, Pageable pageable);
    
    Page<Movie> findByStatusAndIsActiveTrueOrderByReleaseDateDesc(String status, Pageable pageable);
    
    Page<Movie> findByStatusAndIsActiveTrueOrderByReleaseDateAsc(String status, Pageable pageable);
    
    Page<Movie> findByGenresContainingAndIsActiveTrue(String genre, Pageable pageable);
    
    List<Movie> findTop10ByIsActiveTrueOrderByReleaseDateDesc();
    
    @Aggregation(pipeline = {
    "{ $match: { 'isActive': true } }", // Lọc phim active
    "{ $unwind: '$genres' }",           // Tách mỗi genre trong mảng genres thành một document riêng
    "{ $group: { '_id': '$genres' } }", // Nhóm theo genre để lấy distinct
    "{ $sort: { '_id': 1 } }",           // Sắp xếp theo tên genre
    "{ $project: { 'genre': '$_id', '_id': 0 } }" // Đổi tên _id thành genre và chỉ lấy trường genre
    })
    List<GenreProjection> findDistinctGenres(); // Trả về list các object chỉ chứa genre

    // Interface hoặc class để project kết quả
    interface GenreProjection {
        String getGenre();
    }
    
    @Aggregation(pipeline = {
        "{ $match: { 'isActive': true, 'country': { $ne: null, $ne: '' } } }", // Lọc phim active và có country
        "{ $group: { '_id': '$country' } }", // Nhóm theo country để lấy distinct
        "{ $sort: { '_id': 1 } }",           // Sắp xếp theo tên quốc gia
        "{ $project: { 'country': '$_id', '_id': 0 } }" // Đổi tên _id thành country
    })
    List<CountryProjection> findDistinctCountries();

    interface CountryProjection {
        String getCountry();
    }

    @Query(value = "{'isActive': true}", fields = "{'genres': 1, '_id': 0}")
    List<Movie> findActiveMoviesGenres();
    
}