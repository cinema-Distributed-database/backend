package com.cinema.repository;

import com.cinema.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    @Query(value = "{}", fields = "{ 'genres': 1 }")
    List<Movie> findAllGenres();
    
    @Query("{ 'isActive': true }")
    List<Movie> findActiveMovies();
    
    @Query("[ { $match: { 'isActive': true } }, { $unwind: '$genres' }, { $group: { '_id': '$genres' } }, { $project: { '_id': 1 } } ]")
    default List<String> findDistinctGenres() {
        return findAllGenres().stream()
                .flatMap(movie -> movie.getGenres().stream())
                .distinct()
                .sorted()
                .toList();
    }
}