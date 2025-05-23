package com.cinema.config;

import com.cinema.model.*; // Import tất cả các model của bạn
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import jakarta.annotation.PostConstruct; // Sử dụng jakarta.annotation

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        log.info("Starting MongoDB index creation process...");
        createCinemaIndexes();
        createRoomIndexes();
        createMovieIndexes();
        createShowtimeIndexes();
        createBookingIndexes();
        createConcessionIndexes();
        log.info("MongoDB index creation process finished.");
    }

    /**
     * CINEMA INDEXES
     */
    private void createCinemaIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Cinema.class); //

        // Index cho tìm kiếm vị trí địa lý
        // Annotation @GeoSpatialIndexed trong Cinema model sẽ xử lý việc này nếu auto-index-creation=true
        // Nếu bạn muốn tạo tường minh ở đây và tắt auto-index-creation:
        ops.ensureIndex(new GeospatialIndex("location.coordinates") // Giả sử field trong model là "location" và có sub-field "coordinates"
                .named("idx_cinema_location_2dsphere"));
        log.info("Ensured geospatial index: idx_cinema_location_2dsphere on cinemas");

        // Index cho lọc rạp theo thành phố và trạng thái, sắp xếp theo tên
        ops.ensureIndex(new Index()
                .on("city", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC) // Có thể thêm name để hỗ trợ sắp xếp
                .named("idx_cinema_city_status_name"));
        log.info("Created index: idx_cinema_city_status_name on cinemas");

        // Index unique cho slug (nếu bạn có trường slug)
        // ops.ensureIndex(new Index().on("slug", Sort.Direction.ASC).unique().named("idx_cinema_slug_unique"));
        // log.info("Created unique index: idx_cinema_slug_unique on cinemas");

        // Text index cho tìm kiếm tên và địa chỉ rạp
        TextIndexDefinition cinemaTextIndex = TextIndexDefinition.builder()
                .onField("name", 2F) // Ưu tiên tên rạp
                .onField("address")
                .named("idx_cinema_text_search")
                .build();
        ops.ensureIndex(cinemaTextIndex);
        log.info("Created text index: idx_cinema_text_search on cinemas");

        log.info("Cinema indexes ensured.");
    }

    /**
     * ROOM INDEXES
     */
    private void createRoomIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Room.class); //

        // Index cho việc tìm phòng theo cinemaId (rất quan trọng)
        // Annotation @Indexed trong Room model đã xử lý việc này nếu auto-index-creation=true
        // Nếu tạo tường minh:
        ops.ensureIndex(new Index().on("cinemaId", Sort.Direction.ASC).named("idx_room_cinemaId"));
        log.info("Ensured index: idx_room_cinemaId on rooms");

        // Index unique để đảm bảo roomNumber là duy nhất trong một cinemaId
        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("roomNumber", Sort.Direction.ASC)
                .unique()
                .named("idx_room_cinemaId_roomNumber_unique"));
        log.info("Created unique index: idx_room_cinemaId_roomNumber_unique on rooms");

        // Index để lọc phòng theo rạp và trạng thái
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
        IndexOperations ops = mongoTemplate.indexOps(Movie.class); //

        // Index cho việc lọc phim đang chiếu, sắp chiếu, phim mới nhất
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("isActive", Sort.Direction.ASC) // Giả sử có trường này để biết phim có đang active không
                .on("releaseDate", Sort.Direction.DESC)
                .named("idx_movie_status_active_releaseDate"));
        log.info("Created index: idx_movie_status_active_releaseDate on movies");

        // Index cho tìm kiếm phim theo thể loại
        ops.ensureIndex(new Index()
                .on("genres", Sort.Direction.ASC) // MongoDB hỗ trợ index trên mảng
                .named("idx_movie_genres"));
        log.info("Created index: idx_movie_genres on movies");

        // Text index cho tìm kiếm thông tin phim
        TextIndexDefinition movieTextIndex = TextIndexDefinition.builder()
                .onField("title", 2F)
                .onField("originalTitle")
                .onField("description")
                // .onField("directors") // Cân nhắc thêm nếu cần tìm theo đạo diễn
                // .onField("cast")      // Cân nhắc thêm nếu có trường cast và cần tìm
                .named("idx_movie_text_search")
                .build();
        ops.ensureIndex(movieTextIndex);
        log.info("Created text index: idx_movie_text_search on movies");

        log.info("Movie indexes ensured.");
    }

    /**
     * SHOWTIME INDEXES
     */
    private void createShowtimeIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Showtime.class); //

        // Các trường movieId, cinemaId, roomId, showDateTime đã được @Indexed trong model Showtime.java
        // Tạo các compound index tường minh ở đây sẽ tốt hơn cho các query cụ thể.

        // Query chính: Tìm suất chiếu theo phim, rạp và thời gian
        ops.ensureIndex(new Index()
                .on("movieId", Sort.Direction.ASC)
                .on("cinemaId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC) // Thời gian chiếu là quan trọng
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

        // Index để lọc suất chiếu theo trạng thái và thời gian
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_status_datetime"));
        log.info("Created index: idx_showtime_status_datetime on showtimes");

        // Index hỗ trợ cho việc query của SeatHoldExpiryService (tìm ghế holding)
        // Nếu seatStatus là Map, việc index hiệu quả các trường con động là khó.
        // Query trong service sẽ là `findByStatusAndShowDateTimeAfter`, sau đó xử lý logic.
        // Không cần index cụ thể cho `seatStatus.*.holdStartedAt` ở đây.

        log.info("Showtime indexes ensured.");
    }


    /**
     * BOOKING INDEXES
     */
    private void createBookingIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Booking.class); //

        // Tra cứu booking theo mã xác nhận (unique)
        // Annotation @Indexed(unique=true) trong Booking model đã xử lý.
        // Nếu tạo tường minh:
        ops.ensureIndex(new Index().on("confirmationCode", Sort.Direction.ASC).unique().named("idx_booking_confirmationCode_unique"));
        log.info("Ensured unique index: idx_booking_confirmationCode_unique on bookings");

        // Lịch sử đặt vé của khách hàng
        ops.ensureIndex(new Index()
                .on("customerInfo.phone", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC) // Sắp xếp mới nhất lên đầu
                .named("idx_booking_customerPhone_bookingTime"));
        log.info("Created index: idx_booking_customerPhone_bookingTime on bookings");

        ops.ensureIndex(new Index()
                .on("customerInfo.email", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .sparse() // Vì email có thể là tùy chọn
                .named("idx_booking_customerEmail_bookingTime"));
        log.info("Created sparse index: idx_booking_customerEmail_bookingTime on bookings");

        // Tìm booking theo suất chiếu
        // Annotation @Indexed trong Booking model đã xử lý.
        // Nếu tạo tường minh:
        ops.ensureIndex(new Index().on("showtimeId", Sort.Direction.ASC).named("idx_booking_showtimeId"));
        log.info("Ensured index: idx_booking_showtimeId on bookings");

        // Truy vấn liên quan đến trạng thái thanh toán
        ops.ensureIndex(new Index()
                .on("paymentStatus", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.ASC) // Hoặc DESC tùy nhu cầu query
                .named("idx_booking_paymentStatus_bookingTime"));
        log.info("Created index: idx_booking_paymentStatus_bookingTime on bookings");
        
        // Để hỗ trợ query các booking theo ngày tạo
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_booking_createdAt"));
        log.info("Created index: idx_booking_createdAt on bookings");


        log.info("Booking indexes ensured.");
    }

    /**
     * CONCESSION INDEXES
     */
    private void createConcessionIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Concession.class); //

        ops.ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .named("idx_concession_category"));
        log.info("Created index: idx_concession_category on concessions");

        // Nếu bạn thường xuyên tìm concession theo rạp
        ops.ensureIndex(new Index()
                .on("cinemaIds", Sort.Direction.ASC) // Index trên mảng
                .named("idx_concession_cinemaIds"));
        log.info("Created index: idx_concession_cinemaIds on concessions");

        log.info("Concession indexes ensured.");
    }
}