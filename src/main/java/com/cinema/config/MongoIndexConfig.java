package com.cinema.config;

import com.cinema.model.*; // Import tất cả các model của bạn
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import com.mongodb.client.model.IndexOptions;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import jakarta.annotation.PostConstruct;

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
        createPaymentIndexes();
        createConcessionIndexes();
        log.info("MongoDB index creation process finished.");
    }

    /**
     * CINEMA INDEXES
     */
 private void createCinemaIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Cinema.class);
        
        // XÓA INDEX CŨ TRƯỚC KHI TÁI TẠO
        try {
            ops.dropIndex("idx_cinema_location_2dsphere");
            log.info("Dropped existing index: idx_cinema_location_2dsphere");
        } catch (Exception e) {
            log.debug("Index idx_cinema_location_2dsphere not found or already dropped");
        }

        // CÁCH 1: Sử dụng raw MongoDB command (Đảm bảo tạo đúng 2dsphere)
        try {
            mongoTemplate.getCollection("cinemas").createIndex(
                new Document("location", "2dsphere"),
                new IndexOptions().name("idx_cinema_location_2dsphere")
            );
            log.info("Created 2dsphere index using raw MongoDB command: idx_cinema_location_2dsphere");
        } catch (Exception e) {
            log.error("Failed to create 2dsphere index using raw command: {}", e.getMessage());
            
            // CÁCH 2: Fallback - Sử dụng Spring Data MongoDB
            try {
                ops.ensureIndex(new GeospatialIndex("location")
                        .named("idx_cinema_location_2dsphere_fallback"));
                log.info("Created 2dsphere index using GeospatialIndex: idx_cinema_location_2dsphere_fallback");
            } catch (Exception ex) {
                log.error("Failed to create 2dsphere index using GeospatialIndex: {}", ex.getMessage());
            }
        }

        // Index cho lọc rạp theo thành phố và trạng thái, sắp xếp theo tên
        ops.ensureIndex(new Index()
                .on("city", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC)
                .named("idx_cinema_city_status_name"));
        log.info("Created index: idx_cinema_city_status_name on cinemas");

        // Index unique cho slug
        ops.ensureIndex(new Index()
                .on("slug", Sort.Direction.ASC)
                .unique()
                .named("idx_cinema_slug_unique"));
        log.info("Created unique index: idx_cinema_slug_unique on cinemas");

        // Text index cho tìm kiếm tên và địa chỉ rạp
        TextIndexDefinition cinemaTextIndex = TextIndexDefinition.builder()
                .onField("name", 2F)
                .onField("address")
                .named("idx_cinema_text_search")
                .build();
        ops.ensureIndex(cinemaTextIndex);
        log.info("Created text index: idx_cinema_text_search on cinemas");

        log.info("Cinema indexes ensured.");
    }

    private void createBookingIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Booking.class);

        ops.ensureIndex(new Index().on("confirmationCode", Sort.Direction.ASC).unique().named("idx_booking_confirmationCode_unique"));
        log.info("Ensured unique index: idx_booking_confirmationCode_unique on bookings");

        ops.ensureIndex(new Index()
                .on("customerInfo.phone", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .named("idx_booking_customerPhone_bookingTime"));
        log.info("Created index: idx_booking_customerPhone_bookingTime on bookings");

        ops.ensureIndex(new Index()
                .on("customerInfo.email", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .sparse()
                .named("idx_booking_customerEmail_bookingTime"));
        log.info("Created sparse index: idx_booking_customerEmail_bookingTime on bookings");

        ops.ensureIndex(new Index().on("showtimeId", Sort.Direction.ASC).named("idx_booking_showtimeId"));
        log.info("Ensured index: idx_booking_showtimeId on bookings");

        ops.ensureIndex(new Index()
                .on("paymentStatus", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.ASC)
                .named("idx_booking_paymentStatus_bookingTime"));
        log.info("Created index: idx_booking_paymentStatus_bookingTime on bookings");
        
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_booking_createdAt"));
        log.info("Created index: idx_booking_createdAt on bookings");

        log.info("Booking indexes ensured.");
    }

    private void createRoomIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Room.class);
        
        ops.ensureIndex(new Index().on("cinemaId", Sort.Direction.ASC).named("idx_room_cinemaId"));
        log.info("Ensured index: idx_room_cinemaId on rooms");
        
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

    private void createMovieIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Movie.class);
        
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("isActive", Sort.Direction.ASC)
                .on("releaseDate", Sort.Direction.DESC)
                .named("idx_movie_status_active_releaseDate"));
        log.info("Created index: idx_movie_status_active_releaseDate on movies");
        
        ops.ensureIndex(new Index()
                .on("genres", Sort.Direction.ASC)
                .named("idx_movie_genres"));
        log.info("Created index: idx_movie_genres on movies");
        
        TextIndexDefinition movieTextIndex = TextIndexDefinition.builder()
                .onField("title", 2F)
                .onField("originalTitle")
                .onField("description")
                .named("idx_movie_text_search")
                .build();
        ops.ensureIndex(movieTextIndex);
        log.info("Created text index: idx_movie_text_search on movies");
        
        log.info("Movie indexes ensured.");
    }

    private void createShowtimeIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Showtime.class);
        
        ops.ensureIndex(new Index().on("movieId", Sort.Direction.ASC).named("idx_showtime_movieId"));
        ops.ensureIndex(new Index().on("cinemaId", Sort.Direction.ASC).named("idx_showtime_cinemaId"));
        ops.ensureIndex(new Index().on("roomId", Sort.Direction.ASC).named("idx_showtime_roomId"));
        ops.ensureIndex(new Index().on("showDateTime", Sort.Direction.ASC).named("idx_showtime_showDateTime"));

        ops.ensureIndex(new Index()
                .on("movieId", Sort.Direction.ASC)
                .on("cinemaId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_movie_cinema_datetime"));
        log.info("Created index: idx_showtime_movie_cinema_datetime on showtimes");
        
        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_cinema_datetime"));
        log.info("Created index: idx_showtime_cinema_datetime on showtimes");
        
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

        ops.ensureIndex(new Index()
                .on("hasHoldingSeats", Sort.Direction.ASC)
                .named("idx_showtime_hasHoldingSeats"));
        log.info("Created index: idx_showtime_hasHoldingSeats on showtimes");
        
        log.info("Showtime indexes ensured.");
    }

    private void createConcessionIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Concession.class);
        
        ops.ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .named("idx_concession_category"));
        log.info("Created index: idx_concession_category on concessions");
        
        ops.ensureIndex(new Index()
                .on("cinemaIds", Sort.Direction.ASC)
                .named("idx_concession_cinemaIds"));
        log.info("Created index: idx_concession_cinemaIds on concessions");
        
        log.info("Concession indexes ensured.");
    }

    // *** ADDED METHOD ***
    private void createPaymentIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Payment.class); // Sử dụng Payment model bạn đã tạo

        ops.ensureIndex(new Index()
                .on("transactionId", Sort.Direction.ASC) // vnp_TxnRef
                .unique() // Mã giao dịch của VNPay nên là duy nhất
                .named("idx_payment_transactionId_unique"));
        log.info("Ensured unique index: idx_payment_transactionId_unique on payments collection");

        ops.ensureIndex(new Index()
                .on("bookingId", Sort.Direction.ASC)
                .named("idx_payment_bookingId"));
        log.info("Ensured index: idx_payment_bookingId on payments collection");

        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_payment_status_createdAt"));
        log.info("Ensured index: idx_payment_status_createdAt on payments collection");
        
        ops.ensureIndex(new Index()
                .on("paymentMethod", Sort.Direction.ASC)
                .named("idx_payment_paymentMethod"));
        log.info("Ensured index: idx_payment_paymentMethod on payments collection");

        log.info("Payment collection indexes ensured.");
    }
}