package com.cinema.service;

import com.cinema.model.Cinema;
import com.cinema.model.Room;
import com.cinema.repository.CinemaRepository;
import com.cinema.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
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
public class CinemaService {
    
    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;
    
    /**
     * Lấy tất cả rạp chiếu
     */
    public Page<Cinema> getAllCinemas(Pageable pageable) {
        return cinemaRepository.findByStatus("active", pageable);
    }
    
    /**
     * Lấy thông tin chi tiết rạp
     */
    public Optional<Cinema> getCinemaById(String id) {
        return cinemaRepository.findById(id);
    }
    
    /**
     * Tìm rạp gần nhất
     */
    public List<Cinema> findNearbyCinemas(double latitude, double longitude, double radiusKm) {
        Point location = new Point(longitude, latitude); // MongoDB sử dụng [longitude, latitude]
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(location, distance);
        
        return cinemaRepository.findByLocationWithin(circle);
    }
    
    /**
     * Lấy danh sách phòng chiếu của rạp
     */
    public List<Room> getRoomsByCinema(String cinemaId) {
        return roomRepository.findByCinemaIdAndStatus(cinemaId, "active");
    }
    
    /**
     * Lấy rạp theo thành phố
     */
    public Page<Cinema> getCinemasByCity(String city, Pageable pageable) {
        return cinemaRepository.findByCityAndStatus(city, "active", pageable);
    }
    
    /**
     * Tìm kiếm rạp theo từ khóa
     */
    public List<Cinema> searchCinemas(String keyword) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
        Query query = TextQuery.queryText(criteria)
                .sortByScore();
        query.addCriteria(Criteria.where("status").is("active"));
        
        return mongoTemplate.find(query, Cinema.class);
    }
    
    /**
     * Lấy danh sách thành phố có rạp
     */
    public List<String> getCitiesWithCinemas() {
        return cinemaRepository.findDistinctCitiesByStatus("active");
    }
    
    /**
     * Tìm rạp theo nhiều điều kiện
     */
    public List<Cinema> searchCinemasWithFilters(String keyword, String city) {
        Query query = new Query();
        
        // Thêm điều kiện tìm kiếm text nếu có keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
            query = TextQuery.queryText(textCriteria).sortByScore();
        }
        
        // Thêm filter theo thành phố
        if (city != null && !city.trim().isEmpty()) {
            query.addCriteria(Criteria.where("city").is(city));
        }
        
        // Chỉ lấy rạp đang hoạt động
        query.addCriteria(Criteria.where("status").is("active"));
        
        return mongoTemplate.find(query, Cinema.class);
    }
    
    /**
     * Lấy rạp có nhiều phòng chiếu nhất
     */
    public List<Cinema> getTopCinemasByRoomCount(int limit) {
        Query query = new Query(Criteria.where("status").is("active"));
        query.limit(limit);
        // Sắp xếp theo số phòng giảm dần
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "roomCount"));
        
        return mongoTemplate.find(query, Cinema.class);
    }
}