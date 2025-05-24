package com.cinema.repository;

import com.cinema.dto.response.BookingAggregatedDetailsDto;
import com.cinema.model.*; // Import các model cần thiết
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookingRepositoryCustomImpl implements BookingRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<BookingAggregatedDetailsDto> findBookingWithDetailsById(String bookingId) {
        MatchOperation matchStage = Aggregation.match(Criteria.where("_id").is(new ObjectId(bookingId)));
        
        // Lookup showtime first to get movieId, cinemaId, roomId
        LookupOperation lookupShowtime = Aggregation.lookup("showtimes", "showtimeId", "_id", "showtimeDetailsAgg");
        UnwindOperation unwindShowtime = Aggregation.unwind("showtimeDetailsAgg", true); // true for preserveNullAndEmptyArrays

        // Lookup movie, cinema, room based on IDs from showtimeDetailsAgg
        LookupOperation lookupMovie = Aggregation.lookup("movies", "showtimeDetailsAgg.movieId", "_id", "movieDetailsAgg");
        UnwindOperation unwindMovie = Aggregation.unwind("movieDetailsAgg", true);

        LookupOperation lookupCinema = Aggregation.lookup("cinemas", "showtimeDetailsAgg.cinemaId", "_id", "cinemaDetailsAgg");
        UnwindOperation unwindCinema = Aggregation.unwind("cinemaDetailsAgg", true);

        LookupOperation lookupRoom = Aggregation.lookup("rooms", "showtimeDetailsAgg.roomId", "_id", "roomDetailsAgg");
        UnwindOperation unwindRoom = Aggregation.unwind("roomDetailsAgg", true);

        // Projection để định hình output DTO, bạn có thể điều chỉnh các trường ở đây
        // Hoặc bạn có thể map thủ công trong Java sau khi lấy kết quả aggregation với các object đã join.
        // Để đơn giản, ví dụ này sẽ trả về các object được join và bạn map trong service.
        // Nếu dùng projection, bạn sẽ cần định nghĩa cấu trúc của BookingAggregatedDetailsDto trong projection.

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                lookupShowtime, unwindShowtime,
                lookupMovie, unwindMovie,
                lookupCinema, unwindCinema,
                lookupRoom, unwindRoom
                // Nếu cần projection cụ thể thì thêm ProjectionOperation ở đây
        );

        AggregationResults<BookingAggregatedResult> results = mongoTemplate.aggregate(
                aggregation, Booking.class, BookingAggregatedResult.class
        );

        BookingAggregatedResult uniqueResult = results.getUniqueMappedResult();
        if (uniqueResult != null) {
            return Optional.of(BookingAggregatedDetailsDto.fromBookingAndAggregatedData(
                uniqueResult, // Original booking fields are mapped by default
                uniqueResult.getMovieDetailsAgg(),
                uniqueResult.getCinemaDetailsAgg(),
                uniqueResult.getRoomDetailsAgg(),
                uniqueResult.getShowtimeDetailsAgg()
            ));
        }
        return Optional.empty();
    }
    // Helper class để hứng kết quả aggregation trước khi map sang DTO
    // Đặt class này bên trong Impl hoặc file riêng nếu muốn
    private static class BookingAggregatedResult extends Booking { // Kế thừa Booking để lấy các trường gốc
        private Movie movieDetailsAgg;
        private Cinema cinemaDetailsAgg;
        private Room roomDetailsAgg;
        private Showtime showtimeDetailsAgg;
        
        public Movie getMovieDetailsAgg() {
            return movieDetailsAgg;
        }
        
        public Cinema getCinemaDetailsAgg() {
            return cinemaDetailsAgg;
        }
        
        public Room getRoomDetailsAgg() {
            return roomDetailsAgg;
        }
        
        public Showtime getShowtimeDetailsAgg() {
            return showtimeDetailsAgg;
        }
    }


    @Override
    public Page<BookingAggregatedDetailsDto> findAllBookingsWithDetails(Pageable pageable) {
        // Tương tự như findBookingWithDetailsById nhưng có thêm $skip và $limit cho phân trang
        // Và không có $match theo ID cụ thể trừ khi có filter.
        // Đây là một ví dụ phức tạp hơn, cần xử lý cả count query cho total elements.

        // Count total documents for pagination
        Aggregation countAggregation = Aggregation.newAggregation(
                // Các $lookup và $unwind tương tự như trên nếu bạn muốn filter dựa trên dữ liệu join
                // hoặc chỉ đơn giản là đếm số booking gốc
                Aggregation.group().count().as("total")
        );
        // Thêm các MatchOperation nếu có filter từ bên ngoài
        // ...

        AggregationResults<TotalCount> countResult = mongoTemplate.aggregate(
                countAggregation, Booking.class, TotalCount.class
        );
        long total = countResult.getUniqueMappedResult() != null ? countResult.getUniqueMappedResult().getTotal() : 0;


        // Actual data fetching aggregation
        LookupOperation lookupShowtime = Aggregation.lookup("showtimes", "showtimeId", "_id", "showtimeDetailsAgg");
        UnwindOperation unwindShowtime = Aggregation.unwind("showtimeDetailsAgg", true);
        LookupOperation lookupMovie = Aggregation.lookup("movies", "showtimeDetailsAgg.movieId", "_id", "movieDetailsAgg");
        UnwindOperation unwindMovie = Aggregation.unwind("movieDetailsAgg", true);
        LookupOperation lookupCinema = Aggregation.lookup("cinemas", "showtimeDetailsAgg.cinemaId", "_id", "cinemaDetailsAgg");
        UnwindOperation unwindCinema = Aggregation.unwind("cinemaDetailsAgg", true);
        LookupOperation lookupRoom = Aggregation.lookup("rooms", "showtimeDetailsAgg.roomId", "_id", "roomDetailsAgg");
        UnwindOperation unwindRoom = Aggregation.unwind("roomDetailsAgg", true);
        
        SkipOperation skip = Aggregation.skip(pageable.getOffset());
        LimitOperation limit = Aggregation.limit(pageable.getPageSize());
        // Thêm SortOperation nếu cần thiết từ pageable.getSort()

        Aggregation dataAggregation = Aggregation.newAggregation(
                // Các MatchOperation nếu có filter
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