package com.cinema.repository;

import com.cinema.model.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Circle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaRepository extends MongoRepository<Cinema, String> {
    
    Page<Cinema> findByStatus(String status, Pageable pageable);
    
    Page<Cinema> findByCityAndStatus(String city, String status, Pageable pageable);
    
    List<Cinema> findByLocationWithin(Circle circle);
    
    @Query("{ 'location': { $near: { $geometry: { type: 'Point', coordinates: [?1, ?0] }, $maxDistance: ?2 } }, 'status': 'active' }")
    List<Cinema> findNearbyActive(double latitude, double longitude, double maxDistanceMeters);
    
// Trong CinemaRepository.java
    @Query("db.cinemas.distinct('city', { 'status': ?0 })")
    List<String> findDistinctCitiesByStatus(String status);
    
    List<Cinema> findByCityContainingIgnoreCaseAndStatus(String city, String status);
    
    List<Cinema> findByNameContainingIgnoreCaseAndStatus(String name, String status);
    
    @Query("{ 'status': 'active', 'roomCount': { $exists: true } }")
    List<Cinema> findActiveWithRoomCount();
}