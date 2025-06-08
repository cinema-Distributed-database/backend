package com.cinema.repository;

import com.cinema.dto.response.BookingAggregatedDetailsDto;
import com.cinema.model.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookingRepositoryCustomImpl implements BookingRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    /**
     * Helper method để tạo một AggregationOperation cho $lookup
     * có khả năng xử lý ID dạng String (cả ObjectId và slug) một cách an toàn.
     */
private AggregationOperation createPipelineLookup(String from, String localField, String as) {
    return context -> new Document("$lookup",
            new Document("from", from)
                    .append("let", new Document("lookupId", "$" + localField))
                    .append("pipeline", Arrays.asList(
                            new Document("$match",
                                    new Document("$expr",
                                            // *** TOÀN BỘ LOGIC NẰM Ở ĐÂY ***
                                            new Document("$or", Arrays.asList(
                                                    // Điều kiện 1: Tra cứu bằng ObjectId
                                                    new Document("$eq", Arrays.asList(
                                                            "$_id",
                                                            new Document("$convert", new Document("input", "$$lookupId")
                                                                    .append("to", "objectId")
                                                                    .append("onError", null)
                                                                    .append("onNull", null)
                                                            )
                                                    )),
                                                    // Điều kiện 2: Tra cứu bằng String
                                                    new Document("$eq", Arrays.asList("$_id", "$$lookupId"))
                                            ))
                                    )
                            )
                    ))
                    .append("as", as)
    );
}

    @Override
    public Optional<BookingAggregatedDetailsDto> findBookingWithDetailsById(String bookingId) {
        if (!ObjectId.isValid(bookingId)) {
            return Optional.empty();
        }

        MatchOperation matchStage = Aggregation.match(Criteria.where("_id").is(new ObjectId(bookingId)));

        AggregationOperation lookupShowtime = createPipelineLookup("showtimes", "showtimeId", "showtimeDetailsAgg");
        UnwindOperation unwindShowtime = Aggregation.unwind("showtimeDetailsAgg", true);

        AggregationOperation lookupMovie = createPipelineLookup("movies", "showtimeDetailsAgg.movieId", "movieDetailsAgg");
        UnwindOperation unwindMovie = Aggregation.unwind("movieDetailsAgg", true);

        AggregationOperation lookupCinema = createPipelineLookup("cinemas", "showtimeDetailsAgg.cinemaId", "cinemaDetailsAgg");
        UnwindOperation unwindCinema = Aggregation.unwind("cinemaDetailsAgg", true);

        AggregationOperation lookupRoom = createPipelineLookup("rooms", "showtimeDetailsAgg.roomId", "roomDetailsAgg");
        UnwindOperation unwindRoom = Aggregation.unwind("roomDetailsAgg", true);

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                lookupShowtime, unwindShowtime,
                lookupMovie, unwindMovie,
                lookupCinema, unwindCinema,
                lookupRoom, unwindRoom
        );

        AggregationResults<BookingAggregatedResult> results = mongoTemplate.aggregate(
                aggregation, Booking.class, BookingAggregatedResult.class
        );

        BookingAggregatedResult uniqueResult = results.getUniqueMappedResult();
        if (uniqueResult != null) {
            return Optional.of(BookingAggregatedDetailsDto.fromBookingAndAggregatedData(
                uniqueResult,
                uniqueResult.getMovieDetailsAgg(),
                uniqueResult.getCinemaDetailsAgg(),
                uniqueResult.getRoomDetailsAgg(),
                uniqueResult.getShowtimeDetailsAgg()
            ));
        }
        return Optional.empty();
    }

    @Data
    private static class BookingAggregatedResult extends Booking {
        private Movie movieDetailsAgg;
        private Cinema cinemaDetailsAgg;
        private Room roomDetailsAgg;
        private Showtime showtimeDetailsAgg;
    }

    @Override
    public Page<BookingAggregatedDetailsDto> findAllBookingsWithDetails(Pageable pageable) {
        Aggregation countAggregation = Aggregation.newAggregation(
                Aggregation.group().count().as("total")
        );
        AggregationResults<TotalCount> countResult = mongoTemplate.aggregate(
                countAggregation, Booking.class, TotalCount.class
        );
        long total = countResult.getUniqueMappedResult() != null ? countResult.getUniqueMappedResult().getTotal() : 0;

        AggregationOperation lookupShowtime = createPipelineLookup("showtimes", "showtimeId", "showtimeDetailsAgg");
        UnwindOperation unwindShowtime = Aggregation.unwind("showtimeDetailsAgg", true);
        AggregationOperation lookupMovie = createPipelineLookup("movies", "showtimeDetailsAgg.movieId", "movieDetailsAgg");
        UnwindOperation unwindMovie = Aggregation.unwind("movieDetailsAgg", true);
        AggregationOperation lookupCinema = createPipelineLookup("cinemas", "showtimeDetailsAgg.cinemaId", "cinemaDetailsAgg");
        UnwindOperation unwindCinema = Aggregation.unwind("cinemaDetailsAgg", true);
        AggregationOperation lookupRoom = createPipelineLookup("rooms", "showtimeDetailsAgg.roomId", "roomDetailsAgg");
        UnwindOperation unwindRoom = Aggregation.unwind("roomDetailsAgg", true);
        
        SkipOperation skip = Aggregation.skip(pageable.getOffset());
        LimitOperation limit = Aggregation.limit(pageable.getPageSize());
        
        Aggregation dataAggregation = Aggregation.newAggregation(
                lookupShowtime, unwindShowtime,
                lookupMovie, unwindMovie,
                lookupCinema, unwindCinema,
                lookupRoom, unwindRoom,
                skip,
                limit
        );
        
        AggregationResults<BookingAggregatedResult> results = mongoTemplate.aggregate(
                dataAggregation, Booking.class, BookingAggregatedResult.class
        );

        List<BookingAggregatedDetailsDto> dtoList = results.getMappedResults().stream()
                .map(res -> BookingAggregatedDetailsDto.fromBookingAndAggregatedData(
                        res,
                        res.getMovieDetailsAgg(),
                        res.getCinemaDetailsAgg(),
                        res.getRoomDetailsAgg(),
                        res.getShowtimeDetailsAgg()
                ))
                .toList();
        
        return new PageImpl<>(dtoList, pageable, total);
    }

    @Data
    private static class TotalCount {
        private long total;
    }
}