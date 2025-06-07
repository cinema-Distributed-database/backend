package com.cinema.service;

import com.cinema.dto.response.ShowtimeSummaryDto;
import com.cinema.enums.ShowtimeStatus;
import com.cinema.model.Cinema; // <<< THAY ĐỔI: Import model Cinema
import com.cinema.model.Showtime;
import com.cinema.repository.CinemaRepository; // <<< THAY ĐỔI: Import CinemaRepository
import com.cinema.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map; // <<< THAY ĐỔI: Import Map
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final CinemaRepository cinemaRepository; // <<< THAY ĐỔI: Inject CinemaRepository

    /**
     * Lấy danh sách suất chiếu có filter (trả về DTO tóm tắt).
     */
    public List<ShowtimeSummaryDto> getShowtimes(String movieIdStr, String cinemaIdStr, String city, LocalDate startDate, LocalDate endDate, String statusString) {
        log.info("Request lấy suất chiếu - movieId: {}, cinemaId: {}, city: {}, startDate: {}, endDate: {}, status: {}",
                movieIdStr, cinemaIdStr, city, startDate, endDate, statusString);

        ShowtimeStatus status = (statusString != null && !statusString.trim().isEmpty())
                ? ShowtimeStatus.fromValue(statusString.trim())
                : ShowtimeStatus.ACTIVE;

        List<Showtime> showtimes = showtimeRepository.findShowtimesByFlexibleFilters(movieIdStr, cinemaIdStr, city, startDate, endDate, status);

        if (showtimes.isEmpty()) {
            log.info("Không tìm thấy suất chiếu nào với các điều kiện đã cho.");
            return Collections.emptyList();
        }

        // 1. Lấy danh sách các cinemaId duy nhất từ kết quả suất chiếu
        List<String> cinemaIds = showtimes.stream()
                .map(Showtime::getCinemaId)
                .distinct()
                .collect(Collectors.toList());

        // 2. Gọi DB một lần duy nhất để lấy tất cả thông tin rạp phim cần thiết
        Map<String, Cinema> cinemaMap = cinemaRepository.findAllById(cinemaIds).stream()
                .collect(Collectors.toMap(Cinema::getId, cinema -> cinema));

        log.info("Đã tìm thấy {} suất chiếu và thông tin của {} rạp phim tương ứng.", showtimes.size(), cinemaMap.size());
        
        // 3. Chuyển đổi Showtime sang DTO và điền thêm thông tin từ rạp phim
        return showtimes.stream()
                .map(showtime -> {
                    ShowtimeSummaryDto dto = new ShowtimeSummaryDto();
                    dto.setId(showtime.getId());
                    dto.setMovieId(showtime.getMovieId());
                    dto.setCinemaId(showtime.getCinemaId());
                    dto.setRoomId(showtime.getRoomId());
                    dto.setShowDateTime(showtime.getShowDateTime());
                    dto.setScreenType(showtime.getScreenType());
                    dto.setTotalSeats(showtime.getTotalSeats());
                    dto.setAvailableSeats(showtime.getAvailableSeats());
                    dto.setStatus(showtime.getStatus());

                    // Lấy thông tin rạp từ Map đã truy vấn
                    Cinema cinema = cinemaMap.get(showtime.getCinemaId());
                    if (cinema != null) {
                        dto.setCinemaName(cinema.getName());
                        dto.setCinemaAddress(cinema.getAddress());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Optional<Showtime> getShowtimeById(String id) {
        log.info("Request lấy chi tiết suất chiếu ID: {}", id);
        return showtimeRepository.findByIdAndStatus(id, ShowtimeStatus.ACTIVE);
    }

    // --- CÁC PHƯƠNG THỨC DEBUG/TEST ---
    public List<Showtime> getAllShowtimes() {
        log.info("DEBUG: Lấy tất cả suất chiếu trong DB.");
        return showtimeRepository.findAll();
    }

    public List<Showtime> getShowtimesByMovieIdOnly(String movieId) {
        log.info("DEBUG: Lấy suất chiếu theo movieId: {}", movieId);
        return showtimeRepository.findByMovieId(movieId);
    }
}