package com.cinema.service;

import com.cinema.config.AppProperties;
import com.cinema.dto.response.SeatStatusDto;
import com.cinema.model.Showtime;
import com.cinema.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final ShowtimeRepository showtimeRepository;
    private final AppProperties appProperties; // Để lấy HOLD_EXPIRY_MINUTES

    // Cache không còn cần thiết nếu dựa hoàn toàn vào DB state và scheduled job
    // private final Map<String, LocalDateTime> seatHoldCache = new ConcurrentHashMap<>();

    /**
     * Chạy định kỳ để kiểm tra và giải phóng ghế hết hạn giữ.
     */
@Scheduled(fixedRateString = "${cinema.seat-hold.expiry-check-rate-ms:60000}")
    @Transactional
    public void releaseExpiredSeatHolds() {
        log.debug("Checking for expired seat holds...");
        int holdExpiryMinutes = appProperties.getSeatHold().getExpiryMinutes();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.minusMinutes(holdExpiryMinutes);

        // Sử dụng phương thức repository mới
        List<Showtime> showtimesWithHoldingSeats = showtimeRepository.findByHasHoldingSeatsTrue(); // THAY ĐỔI Ở ĐÂY

        for (Showtime showtime : showtimesWithHoldingSeats) {
            boolean hasChangesToSeatStatus = false;
            Map<String, Showtime.SeatStatus> seatStatusMap = showtime.getSeatStatus();
            int currentHoldingCount = 0; // Đếm số ghế còn đang giữ sau khi kiểm tra

            if (seatStatusMap != null && !seatStatusMap.isEmpty()) {
                for (Map.Entry<String, Showtime.SeatStatus> entry : seatStatusMap.entrySet()) {
                    String seatId = entry.getKey();
                    Showtime.SeatStatus status = entry.getValue();

                    if ("holding".equals(status.getStatus())) { // Sử dụng hằng số/enum nếu có
                        if (status.getHoldStartedAt() != null &&
                            status.getHoldStartedAt().isBefore(expiryThreshold)) {

                            status.setStatus("available"); // Sử dụng hằng số/enum nếu có
                            status.setHoldStartedAt(null);
                            status.setBookingId(null);
                            hasChangesToSeatStatus = true;
                            log.info("Released expired seat hold: ShowtimeID={}, SeatID={}", showtime.getId(), seatId);
                        } else {
                            currentHoldingCount++; // Ghế này vẫn đang được giữ hợp lệ
                        }
                    }
                }
            }

            if (hasChangesToSeatStatus) {
                updateAvailableSeatsCount(showtime); // Cập nhật số ghế trống
                // Cập nhật lại cờ hasHoldingSeats
                showtime.setHasHoldingSeats(currentHoldingCount > 0); // THAY ĐỔI Ở ĐÂY
                showtimeRepository.save(showtime);
            } else if (currentHoldingCount == 0 && showtime.isHasHoldingSeats()) {
                // Trường hợp không có thay đổi nào về trạng thái ghế (không có ghế nào hết hạn)
                // nhưng trước đó hasHoldingSeats là true và giờ không còn ghế nào holding nữa
                // (ví dụ: tất cả ghế holding đã được release bởi user hoặc confirm booking)
                // Cần đảm bảo cờ này được cập nhật đúng ở các hàm holdSeats, releaseSeats, confirmSeatBooking.
                // Dòng này có thể không cần nếu các hàm khác đã cập nhật đúng.
                // showtime.setHasHoldingSeats(false);
                // showtimeRepository.save(showtime);
            }
        }
    }

    /**
     * Lấy trạng thái ghế của một suất chiếu.
     */
    public Optional<SeatStatusDto> getSeatStatusForShowtime(String showtimeId) {
        return showtimeRepository.findById(showtimeId).map(showtime -> {
            SeatStatusDto dto = new SeatStatusDto();
            dto.setShowtimeId(showtimeId);
            dto.setSeatStatus(showtime.getSeatStatus() != null ? showtime.getSeatStatus() : new ConcurrentHashMap<>());
            dto.setTotalSeats(showtime.getTotalSeats());
            
            if (showtime.getSeatStatus() != null) {
                long holdingCount = showtime.getSeatStatus().values().stream().filter(s -> "holding".equals(s.getStatus())).count();
                long bookedCount = showtime.getSeatStatus().values().stream().filter(s -> "booked".equals(s.getStatus())).count();
                dto.setHoldingSeats((int) holdingCount);
                dto.setBookedSeats((int) bookedCount);
                dto.setAvailableSeats(showtime.getTotalSeats() - (int) holdingCount - (int) bookedCount);
            } else {
                dto.setAvailableSeats(showtime.getTotalSeats());
                dto.setHoldingSeats(0);
                dto.setBookedSeats(0);
            }
            return dto;
        });
    }


    /**
     * Giữ ghế cho khách hàng.
     */
    @Transactional
    public boolean holdSeats(String showtimeId, List<String> seatIds, String customerPhone) {
        // ... (logic kiểm tra showtime, ghế như cũ) ...
        
        // Giữ tất cả ghế
        for (String seatId : seatIds) {
            Showtime.SeatStatus newStatus = new Showtime.SeatStatus();
            newStatus.setStatus(SeatStatusValue.HOLDING); // Giả sử có Enum SeatStatusValue
            newStatus.setHoldStartedAt(now);
            seatStatusMap.put(seatId, newStatus);
            log.debug("Đã giữ ghế: ShowtimeID={}, SeatID={}, HeldAt={}", showtimeId, seatId, now);
        }

        updateAvailableSeatsCount(showtime);
        showtime.setHasHoldingSeats(true); // CẬP NHẬT CỜ KHI GIỮ GHẾ
        showtimeRepository.save(showtime);
        log.info("Đã giữ thành công {} ghế cho Showtime {}: {}", seatIds.size(), showtimeId, seatIds);
        return true;
    }

    /**
     * Hủy giữ ghế (khách hàng tự hủy hoặc admin hủy).
     */
    @Transactional
    public boolean releaseSeats(String showtimeId, List<String> seatIds) {
        log.info("Attempting to release seats for showtimeId: {}, seats: {}", showtimeId, seatIds);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Showtime không tồn tại: " + showtimeId));

        Map<String, Showtime.SeatStatus> seatStatusMap = showtime.getSeatStatus();
        if (seatStatusMap == null) {
            log.warn("Không có bản đồ trạng thái ghế cho Showtime {}. Không có ghế nào được giải phóng.", showtimeId);
            return false; // Không có gì để giải phóng
        }

        boolean releasedAny = false;
        for (String seatId : seatIds) {
            Showtime.SeatStatus currentStatus = seatStatusMap.get(seatId);
            if (currentStatus != null && "holding".equals(currentStatus.getStatus())) {
                // Chỉ giải phóng ghế đang "holding"
                currentStatus.setStatus("available");
                currentStatus.setHoldStartedAt(null);
                currentStatus.setBookingId(null);
                log.debug("Đã giải phóng ghế: ShowtimeID={}, SeatID={}", showtimeId, seatId);
                releasedAny = true;
            } else {
                log.warn("Không thể giải phóng ghế {} cho Showtime {}. Trạng thái hiện tại: {}", 
                         seatId, showtimeId, (currentStatus != null ? currentStatus.getStatus() : "không tồn tại"));
            }
        }

        if (releasedAny) {
            updateAvailableSeatsCount(showtime);
            showtimeRepository.save(showtime);
            log.info("Đã giải phóng thành công một số ghế cho Showtime {}: {}", showtimeId, seatIds.stream().filter(s -> seatStatusMap.get(s) != null && "available".equals(seatStatusMap.get(s).getStatus())).collect(Collectors.toList()));
        }
        return releasedAny;
    }

    /**
     * Gia hạn thời gian giữ ghế.
     */
    @Transactional
    public boolean extendSeatHold(String showtimeId, List<String> seatIds) {
        log.info("Attempting to extend seat hold for showtimeId: {}, seats: {}", showtimeId, seatIds);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Showtime không tồn tại: " + showtimeId));

        Map<String, Showtime.SeatStatus> seatStatusMap = showtime.getSeatStatus();
        if (seatStatusMap == null) {
            log.warn("Không có bản đồ trạng thái ghế cho Showtime {}. Không thể gia hạn.", showtimeId);
            return false;
        }
        
        int holdExpiryMinutes = appProperties.getSeatHold().getExpiryMinutes();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpiryThreshold = now.minusMinutes(holdExpiryMinutes);

        boolean extendedAny = false;
        for (String seatId : seatIds) {
            Showtime.SeatStatus currentStatus = seatStatusMap.get(seatId);
            // Chỉ gia hạn ghế đang "holding" và chưa hết hạn (hoặc có một khoảng thời gian gia hạn cho phép)
            if (currentStatus != null && "holding".equals(currentStatus.getStatus())) {
                if (currentStatus.getHoldStartedAt() != null && currentStatus.getHoldStartedAt().isAfter(currentExpiryThreshold)) {
                     currentStatus.setHoldStartedAt(now); // Reset thời gian bắt đầu giữ
                     log.debug("Đã gia hạn giữ ghế: ShowtimeID={}, SeatID={}, NewHoldStartAt={}", showtimeId, seatId, now);
                     extendedAny = true;
                } else {
                    log.warn("Không thể gia hạn ghế {} cho Showtime {}. Ghế đã hết hạn giữ hoặc không ở trạng thái holding.", seatId, showtimeId);
                    // Tùy chọn: có thể giải phóng ghế này luôn nếu đã hết hạn
                    // releaseSeats(showtimeId, List.of(seatId)); 
                    // throw new IllegalStateException("Không thể gia hạn ghế " + seatId + " do đã hết hạn giữ.");
                }
            } else {
                 log.warn("Không thể gia hạn ghế {} cho Showtime {}. Ghế không ở trạng thái holding.", seatId, showtimeId);
            }
        }

        if (extendedAny) {
            // Không cần cập nhật availableSeats vì số ghế holding/booked không đổi
            showtimeRepository.save(showtime);
            log.info("Đã gia hạn thành công thời gian giữ cho một số ghế của Showtime {}: {}", showtimeId, seatIds);
        }
        return extendedAny;
    }
    
    /**
     * Xác nhận đặt ghế (chuyển từ holding sang booked).
     * Phương thức này được gọi bởi BookingService sau khi booking được tạo.
     */
    @Transactional
    public boolean confirmSeatBooking(String showtimeId, List<String> seatIds, String bookingId) {
        // Logic này đã có trong SeatHoldExpiryService cũ, bạn có thể di chuyển và điều chỉnh nếu cần.
        log.info("Attempting to confirm seat booking for showtimeId: {}, seats: {}, bookingId: {}", showtimeId, seatIds, bookingId);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Showtime không tồn tại: " + showtimeId));

        Map<String, Showtime.SeatStatus> seatStatusMap = showtime.getSeatStatus();
        if (seatStatusMap == null) {
            log.error("Không có bản đồ trạng thái ghế cho Showtime {}. Không thể xác nhận đặt vé.", showtimeId);
            throw new IllegalStateException("Lỗi hệ thống: không tìm thấy thông tin ghế của suất chiếu.");
        }

        for (String seatId : seatIds) {
            Showtime.SeatStatus currentStatus = seatStatusMap.get(seatId);
            if (currentStatus == null || !"holding".equals(currentStatus.getStatus())) {
                // Nếu ghế không được tìm thấy hoặc không ở trạng thái "holding" (có thể đã bị người khác đặt hoặc hết hạn)
                log.error("Không thể xác nhận ghế {}: không ở trạng thái 'holding' hoặc không tồn tại. Showtime: {}, Booking: {}", seatId, showtimeId, bookingId);
                // Xử lý lỗi: có thể hủy booking hoặc thông báo lỗi nghiêm trọng
                throw new IllegalStateException("Ghế " + seatId + " không thể xác nhận. Vui lòng thử lại.");
            }
            currentStatus.setStatus(SeatStatusValue.BOOKED);
            currentStatus.setBookingId(bookingId);
            currentStatus.setHoldStartedAt(null);
        }
        
        boolean stillHasHolding = showtime.getSeatStatus().values().stream()
                                      .anyMatch(s -> SeatStatusValue.HOLDING.equals(s.getStatus()));
        showtime.setHasHoldingSeats(stillHasHolding); // CẬP NHẬT CỜ
        showtimeRepository.save(showtime);
        log.info("Đã xác nhận thành công đặt {} ghế cho Showtime {}, BookingID {}", seatIds.size(), showtimeId, bookingId);
        return true;
    }


    /**
     * Cập nhật số ghế trống trong model Showtime.
     */
    private void updateAvailableSeatsCount(Showtime showtime) {
        if (showtime.getSeatStatus() == null || showtime.getSeatStatus().isEmpty()) {
            showtime.setAvailableSeats(showtime.getTotalSeats());
            return;
        }
        long unavailableCount = showtime.getSeatStatus().values().stream()
                .filter(status -> "holding".equals(status.getStatus()) || "booked".equals(status.getStatus()))
                .count();
        showtime.setAvailableSeats(showtime.getTotalSeats() - (int) unavailableCount);
        log.debug("Cập nhật số ghế trống cho Showtime {}: Tổng={}, Không khả dụng={}, Trống={}",
                  showtime.getId(), showtime.getTotalSeats(), unavailableCount, showtime.getAvailableSeats());
    }
}