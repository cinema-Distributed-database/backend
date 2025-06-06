package com.cinema.repository;

import com.cinema.enums.ShowtimeStatus;
import com.cinema.model.Showtime;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ShowtimeRepositoryCustomImpl implements ShowtimeRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Showtime> findShowtimesByFlexibleFilters(String movieIdStr, String cinemaIdStr, LocalDate date, ShowtimeStatus status) {
        Query query = new Query();
        List<Criteria> allCriteria = new ArrayList<>();

        // 1. Xử lý movieId (String hoặc ObjectId)
        if (movieIdStr != null && !movieIdStr.trim().isEmpty()) {
            // Tạo một $or condition
            List<Criteria> movieIdCriteria = new ArrayList<>();
            // Luôn tìm kiếm dạng String
            movieIdCriteria.add(Criteria.where("movieId").is(movieIdStr));
            // Nếu chuỗi hợp lệ, tìm kiếm thêm dạng ObjectId
            if (ObjectId.isValid(movieIdStr)) {
                movieIdCriteria.add(Criteria.where("movieId").is(new ObjectId(movieIdStr)));
            }
            allCriteria.add(new Criteria().orOperator(movieIdCriteria));
        }

        // 2. Xử lý cinemaId (tương tự movieId)
        if (cinemaIdStr != null && !cinemaIdStr.trim().isEmpty()) {
            List<Criteria> cinemaIdCriteria = new ArrayList<>();
            cinemaIdCriteria.add(Criteria.where("cinemaId").is(cinemaIdStr));
            if (ObjectId.isValid(cinemaIdStr)) {
                cinemaIdCriteria.add(Criteria.where("cinemaId").is(new ObjectId(cinemaIdStr)));
            }
            allCriteria.add(new Criteria().orOperator(cinemaIdCriteria));
        }
        
        // 3. Xử lý khoảng thời gian (date)
        if (date != null) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            allCriteria.add(Criteria.where("showDateTime").gte(startOfDay).lte(endOfDay));
        }

        // 4. Xử lý trạng thái (status)
        if (status != null) {
            // Sử dụng converter đã đăng ký, ta chỉ cần truyền enum
            allCriteria.add(Criteria.where("status").is(status));
        }

        // Kết hợp tất cả các điều kiện bằng $and
        if (!allCriteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(allCriteria));
        }

        return mongoTemplate.find(query, Showtime.class);
    }
}