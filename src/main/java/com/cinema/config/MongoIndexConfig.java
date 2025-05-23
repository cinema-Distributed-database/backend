package com.cinema.config;

import com.cinema.model.*; // Import tất cả các model của bạn
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import jakarta.annotation.PostConstruct; // Sử dụng jakarta.annotation
// Cân nhắc import thêm cho TTL index nếu bạn quyết định dùng cho SeatHolds riêng
// import org.springframework.data.mongodb.core.index.PartialIndexFilter;
// import org.springframework.data.mongodb.core.query.Criteria;
// import java.time.Duration;


@Configuration
@Slf4j
@RequiredArgsConstructor // Sử dụng constructor injection cho MongoTemplate
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate; // Constructor injection

    @PostConstruct
    public void initIndexes() {
        log.info("Starting MongoDB index creation process...");
        createCinemaIndexes();
        createRoomIndexes();
        createMovieIndexes();
        createShowtimeIndexes();
        // Không còn User collection riêng theo cấu trúc mới của bạn
        createBookingIndexes();
        createConcessionIndexes();
        log.info("MongoDB index creation process finished.");
    }

    /**
     * CINEMA INDEXES
     */
    private void createCinemaIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Cinema.class);

        ops.ensureIndex(new Index()
                .on("city", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_cinema_city_status"));
        log.info("Created index: idx_cinema_city_status on cinemas");

        // GeoSpatial Index đã được tạo bằng @GeoSpatialIndexed trong model Cinema.java
        // Nếu bạn muốn tạo bằng MongoTemplate, bạn có thể làm như sau,
        // nhưng đảm bảo không tạo trùng lặp nếu đã có annotation.
        // ops.ensureIndex(new GeospatialIndex("location.coordinates") // Hoặc chỉ "location" nếu field là GeoJsonPoint
        //         .named("idx_cinema_location_2dsphere"));
        // log.info("Ensured geospatial index: idx_cinema_location_2dsphere on cinemas.location");


        ops.ensureIndex(new Index()
                .on("slug", Sort.Direction.ASC)
                .unique()
                .named("idx_cinema_slug_unique"));
        log.info("Created unique index: idx_cinema_slug_unique on cinemas");

        // Text index (nếu bạn dùng $text search thường xuyên)
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("name", 2F) // Weight cho name cao hơn
                .onField("address")
                .named("idx_cinema_text_search")
                .build();
        ops.ensureIndex(textIndex);
        log.info("Created text index: idx_cinema_text_search on cinemas");

        log.info("Cinema indexes ensured.");
    }

    /**
     * ROOM INDEXES
     */
    private void createRoomIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Room.class);

        // Index cho việc tìm phòng theo rạp là rất quan trọng
        // @Indexed trên cinemaId trong model Room.java đã đảm nhiệm việc này.
        // Nếu muốn tạo bằng MongoTemplate:
        // ops.ensureIndex(new Index().on("cinemaId", Sort.Direction.ASC).named("idx_room_cinemaId"));
        // log.info("Ensured index: idx_room_cinemaId on rooms");


        // Unique constraint cho cinemaId + roomNumber để đảm bảo mỗi phòng trong rạp là duy nhất
        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("roomNumber", Sort.Direction.ASC)
                .unique()
                .named("idx_room_cinemaId_roomNumber_unique"));
        log.info("Created unique index: idx_room_cinemaId_roomNumber_unique on rooms");

        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_room_cinemaId_status"));
        log.info("Created index: idx_room_cinemaId_status on rooms");

        log.info("Room indexes ensured.");
    }

    /**
     * MOVIE INDEXES
     */
    private void createMovieIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Movie.class);

        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("isActive", Sort.Direction.ASC)
                .on("releaseDate", Sort.Direction.DESC) // Sắp xếp phim mới nhất lên đầu
                .named("idx_movie_status_active_releaseDate"));
        log.info("Created index: idx_movie_status_active_releaseDate on movies");

        ops.ensureIndex(new Index()
                .on("genres", Sort.Direction.ASC) // Cho tìm kiếm phim theo thể loại
                .named("idx_movie_genres"));
        log.info("Created index: idx_movie_genres on movies");

        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("title", 2F)
                .onField("originalTitle")
                .onField("description")
                .named("idx_movie_text_search")
                .build();
        ops.ensureIndex(textIndex);
        log.info("Created text index: idx_movie_text_search on movies");

        log.info("Movie indexes ensured.");
    }

    /**
     * SHOWTIME INDEXES - QUAN TRỌNG NHẤT
     */
    private void createShowtimeIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Showtime.class);

        // Các trường movieId, cinemaId, roomId, showDateTime đã được @Indexed trong model Showtime.java
        // Bạn có thể tạo các compound index cụ thể hơn ở đây nếu cần.

        // Query chính: Tìm suất chiếu theo phim, rạp và thời gian
        ops.ensureIndex(new Index()
                .on("movieId", Sort.Direction.ASC)
                .on("cinemaId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_movie_cinema_datetime"));
        log.info("Created index: idx_showtime_movie_cinema_datetime on showtimes");
        
        // Query suất chiếu theo rạp và thời gian
        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_cinema_datetime"));
        log.info("Created index: idx_showtime_cinema_datetime on showtimes");


        // Query suất chiếu theo phòng và thời gian (để tránh trùng lịch phòng)
        ops.ensureIndex(new Index()
                .on("roomId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_room_datetime"));
        log.info("Created index: idx_showtime_room_datetime on showtimes");


        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_status_datetime"));
        log.info("Created index: idx_showtime_status_datetime on showtimes");

        // Nếu bạn có logic dọn dẹp ghế "holding" bằng job và trường "seatStatus.*.holdStartedAt"
        // thì có thể cần index để hỗ trợ query đó, ví dụ:
        // ops.ensureIndex(new Index().on("seatStatus.A01.holdStartedAt", Sort.Direction.ASC).sparse());
        // Tuy nhiên, index trên các key động của Map ("A01", "A02"...) là không khả thi.
        // Bạn sẽ query theo 'status: "on_sale"' rồi xử lý logic trong service.

        log.info("Showtime indexes ensured.");
    }

    /**
     * BOOKING INDEXES
     */
    private void createBookingIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Booking.class);

        // Tra cứu booking theo mã xác nhận (unique) là rất quan trọng
        // @Indexed trên confirmationCode trong model Booking.java đã đảm nhiệm việc này.
        // Nếu muốn tạo bằng MongoTemplate:
        // ops.ensureIndex(new Index().on("confirmationCode", Sort.Direction.ASC).unique().named("idx_booking_confirmationCode_unique"));
        // log.info("Ensured unique index: idx_booking_confirmationCode_unique on bookings");

        // Lịch sử đặt vé của khách hàng (dựa vào phone hoặc email trong customerInfo)
        ops.ensureIndex(new Index()
                .on("customerInfo.phone", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .named("idx_booking_customerPhone_bookingTime"));
        log.info("Created index: idx_booking_customerPhone_bookingTime on bookings");
        
        ops.ensureIndex(new Index()
                .on("customerInfo.email", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .sparse() // Email là tùy chọn
                .named("idx_booking_customerEmail_bookingTime"));
        log.info("Created sparse index: idx_booking_customerEmail_bookingTime on bookings");

        // Tìm booking theo suất chiếu
        // @Indexed trên showtimeId trong model Booking.java đã đảm nhiệm việc này.
        // Nếu muốn tạo bằng MongoTemplate:
        // ops.ensureIndex(new Index().on("showtimeId", Sort.Direction.ASC).named("idx_booking_showtimeId"));
        // log.info("Ensured index: idx_booking_showtimeId on bookings");


        ops.ensureIndex(new Index()
                .on("paymentStatus", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.ASC)
                .named("idx_booking_paymentStatus_bookingTime"));
        log.info("Created index: idx_booking_paymentStatus_bookingTime on bookings");

        log.info("Booking indexes ensured.");
    }

    /**
     * CONCESSION INDEXES
     */
    private void createConcessionIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Concession.class);

        ops.ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .named("idx_concession_category"));
        log.info("Created index: idx_concession_category on concessions");

        ops.ensureIndex(new Index()
                .on("cinemaIds", Sort.Direction.ASC) // Nếu bạn thường xuyên query concession theo cinemaId
                .named("idx_concession_cinemaIds"));
        log.info("Created index: idx_concession_cinemaIds on concessions (if used for querying)");

        log.info("Concession indexes ensured.");
    }

    // Bạn có thể bỏ các phương thức createMaintenanceIndexes, cleanupExpiredShowtimes, optimizeIndexes
    // và IndexMonitor từ file này nếu bạn muốn tách riêng logic bảo trì và giám sát.
    // Việc dọn dẹp ghế holding đã được chuyển sang SeatHoldExpiryService.java.
}