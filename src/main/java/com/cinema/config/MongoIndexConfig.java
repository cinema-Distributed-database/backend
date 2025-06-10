package com.cinema.config;

import com.cinema.model.*;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import com.mongodb.client.model.IndexOptions;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import jakarta.annotation.PostConstruct;

// @Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        createCinemaIndexes();
        createRoomIndexes();
        createMovieIndexes();
        createShowtimeIndexes();
        createBookingIndexes();
        createPaymentIndexes();
        createConcessionIndexes();
    }

    private void createCinemaIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Cinema.class);
        
        mongoTemplate.getCollection("cinemas").createIndex(
            new Document("location", "2dsphere"),
            new IndexOptions().name("idx_cinema_location_2dsphere")
        );

        ops.ensureIndex(new Index()
                .on("city", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC)
                .named("idx_cinema_city_status_name"));

        ops.ensureIndex(new Index()
                .on("slug", Sort.Direction.ASC)
                .unique()
                .named("idx_cinema_slug_unique"));

        TextIndexDefinition cinemaTextIndex = TextIndexDefinition.builder()
                .onField("name", 2F)
                .onField("address")
                .named("idx_cinema_text_search")
                .build();
        ops.ensureIndex(cinemaTextIndex);
    }

    private void createBookingIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Booking.class);

        ops.ensureIndex(new Index().on("confirmationCode", Sort.Direction.ASC).unique().named("idx_booking_confirmationCode_unique"));

        ops.ensureIndex(new Index()
                .on("customerInfo.phone", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .named("idx_booking_customerPhone_bookingTime"));

        ops.ensureIndex(new Index()
                .on("customerInfo.email", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.DESC)
                .sparse()
                .named("idx_booking_customerEmail_bookingTime"));

        ops.ensureIndex(new Index().on("showtimeId", Sort.Direction.ASC).named("idx_booking_showtimeId"));

        ops.ensureIndex(new Index()
                .on("paymentStatus", Sort.Direction.ASC)
                .on("bookingTime", Sort.Direction.ASC)
                .named("idx_booking_paymentStatus_bookingTime"));
        
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_booking_createdAt"));
    }

    private void createRoomIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Room.class);
        
        ops.ensureIndex(new Index().on("cinemaId", Sort.Direction.ASC).named("idx_room_cinemaId"));
        
        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("roomNumber", Sort.Direction.ASC)
                .unique()
                .named("idx_room_cinemaId_roomNumber_unique"));
        
        ops.ensureIndex(new Index()
                .on("cinemaId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_room_cinemaId_status"));
    }

    private void createMovieIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Movie.class);
        
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("isActive", Sort.Direction.ASC)
                .on("releaseDate", Sort.Direction.DESC)
                .named("idx_movie_status_active_releaseDate"));
        
        ops.ensureIndex(new Index()
                .on("genres", Sort.Direction.ASC)
                .named("idx_movie_genres"));
        
        TextIndexDefinition movieTextIndex = TextIndexDefinition.builder()
                .onField("title", 2F)
                .onField("originalTitle")
                .onField("description")
                .named("idx_movie_text_search")
                .build();
        ops.ensureIndex(movieTextIndex);
    }

    private void createShowtimeIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Showtime.class);
        
        ops.ensureIndex(new Index()
                .on("movieId", Sort.Direction.ASC)
                .on("cinemaId", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_movie_cinema_datetime"));
        
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("showDateTime", Sort.Direction.ASC)
                .named("idx_showtime_status_datetime"));

        ops.ensureIndex(new Index()
                .on("hasHoldingSeats", Sort.Direction.ASC)
                .named("idx_showtime_hasHoldingSeats"));
    }

    private void createConcessionIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Concession.class);
        
        ops.ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .named("idx_concession_category"));
        
        ops.ensureIndex(new Index()
                .on("cinemaIds", Sort.Direction.ASC)
                .named("idx_concession_cinemaIds"));
    }

    private void createPaymentIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Payment.class);

        ops.ensureIndex(new Index()
                .on("transactionId", Sort.Direction.ASC)
                .unique()
                .named("idx_payment_transactionId_unique"));

        ops.ensureIndex(new Index()
                .on("bookingId", Sort.Direction.ASC)
                .named("idx_payment_bookingId"));

        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_payment_status_createdAt"));
        
        ops.ensureIndex(new Index()
                .on("paymentMethod", Sort.Direction.ASC)
                .named("idx_payment_paymentMethod"));
    }
}