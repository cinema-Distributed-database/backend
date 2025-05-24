package com.cinema.repository;

import com.cinema.model.Concession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConcessionRepository extends MongoRepository<Concession, String> {
    List<Concession> findByAvailabilityTrue();
    List<Concession> findByCategoryAndAvailabilityTrue(String category);
    // Tìm concession áp dụng cho một cinema cụ thể và còn hàng
    List<Concession> findByCinemaIdsContainingAndAvailabilityTrue(String cinemaId);
}