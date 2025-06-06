package com.cinema.repository;

import com.cinema.enums.ShowtimeStatus;
import com.cinema.model.Showtime;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeRepositoryCustom {
    /**
     * Tìm kiếm suất chiếu một cách linh hoạt, chấp nhận movieId có thể là String hoặc ObjectId
     * và các bộ lọc khác.
     *
     * @param movieIdStr ID của phim (có thể là String hoặc ObjectId hex string)
     * @param cinemaIdStr ID của rạp (có thể là String hoặc ObjectId hex string)
     * @param date Ngày chiếu
     * @param status Trạng thái suất chiếu
     * @return Danh sách các suất chiếu phù hợp
     */
    List<Showtime> findShowtimesByFlexibleFilters(String movieIdStr, String cinemaIdStr, LocalDate date, ShowtimeStatus status);
}