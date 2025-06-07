package com.cinema.repository;

import com.cinema.enums.ShowtimeStatus;
import com.cinema.model.Cinema; // <<< THAY ĐỔI: Import model Cinema
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
import java.util.stream.Collectors; // <<< THAY ĐỔI: Import Collectors

@Repository
@RequiredArgsConstructor
public class ShowtimeRepositoryCustomImpl implements ShowtimeRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final CinemaRepository cinemaRepository; // <<< THAY ĐỔI: Inject CinemaRepository

    // <<< THAY ĐỔI: Cập nhật toàn bộ phương thức
    @Override
    public List<Showtime> findShowtimesByFlexibleFilters(String movieIdStr, String cinemaIdStr, String city, LocalDate startDate, LocalDate endDate, ShowtimeStatus status) {
        Query query = new Query();
        List<Criteria> allCriteria = new ArrayList<>();

        // 1. Lọc theo Thành phố (city)
        List<String> cinemaIdsFromCity = new ArrayList<>();
        if (city != null && !city.trim().isEmpty()) {
            // Tìm tất cả rạp trong thành phố được chỉ định
            List<Cinema> cinemasInCity = cinemaRepository.findByCityAndStatus(city, "active", null).getContent();
            if (cinemasInCity.isEmpty()) {
                // Nếu không có rạp nào trong thành phố này, không cần tìm suất chiếu nữa
                return List.of();
            }
            // Lấy danh sách ID của các rạp đó
            cinemaIdsFromCity = cinemasInCity.stream().map(Cinema::getId).collect(Collectors.toList());
        }

        // 2. Lọc theo Rạp (cinemaId)
        if (cinemaIdStr != null && !cinemaIdStr.trim().isEmpty()) {
            // Nếu người dùng đã chọn một rạp cụ thể
            if (!cinemaIdsFromCity.isEmpty() && !cinemaIdsFromCity.contains(cinemaIdStr)) {
                // Nếu rạp này không thuộc thành phố đã chọn -> không có kết quả
                return List.of();
            }
            allCriteria.add(Criteria.where("cinemaId").is(cinemaIdStr));
        } else if (!cinemaIdsFromCity.isEmpty()) {
            // Nếu người dùng chỉ chọn thành phố, lọc theo tất cả các rạp trong thành phố đó
            allCriteria.add(Criteria.where("cinemaId").in(cinemaIdsFromCity));
        }


        // 3. Lọc theo Phim (movieId)
        if (movieIdStr != null && !movieIdStr.trim().isEmpty()) {
            List<Criteria> movieIdCriteria = new ArrayList<>();
            movieIdCriteria.add(Criteria.where("movieId").is(movieIdStr));
            if (ObjectId.isValid(movieIdStr)) {
                movieIdCriteria.add(Criteria.where("movieId").is(new ObjectId(movieIdStr)));
            }
            allCriteria.add(new Criteria().orOperator(movieIdCriteria));
        }
        
        // 4. Lọc theo Khoảng thời gian (Date Range)
        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            // Nếu không có endDate, mặc định lọc trong ngày startDate
            LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : startDate.atTime(LocalTime.MAX);
            allCriteria.add(Criteria.where("showDateTime").gte(startDateTime).lte(endDateTime));
        }


        // 5. Lọc theo Trạng thái (status)
        if (status != null) {
            allCriteria.add(Criteria.where("status").is(status));
        }

        // Kết hợp tất cả các điều kiện bằng $and
        if (!allCriteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(allCriteria));
        }
        
        // Sắp xếp kết quả theo thời gian chiếu
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "showDateTime"));

        return mongoTemplate.find(query, Showtime.class);
    }
}