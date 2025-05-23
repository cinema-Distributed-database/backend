package com.cinema.service;

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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldExpiryService {
    
    private final ShowtimeRepository showtimeRepository;
    private final int HOLD_EXPIRY_MINUTES = 10; // Ghế được giữ trong 10 phút
    
    // Cache để theo dõi ghế đang được giữ
    private final Map<String, LocalDateTime> seatHoldCache = new ConcurrentHashMap<>();
    
    /**
     * Chạy mỗi phút để kiểm tra và giải phóng ghế hết hạn
     */
    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây
    @Transactional
    public void releaseExpiredSeatHolds() {
        log.debug("Checking for expired seat holds...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.minusMinutes(HOLD_EXPIRY_MINUTES);
        
        // Tìm tất cả showtime có ghế đang được giữ
        List<Showtime> showtimes = showtimeRepository.findShowtimesWithHoldingSeats();
        
        for (Showtime showtime : showtimes) {
            boolean hasChanges = false;
            Map<String, Showtime.SeatStatus> seatStatus = showtime.getSeatStatus();
            
            if (seatStatus != null) {
                for (Map.Entry<String, Showtime.SeatStatus> entry : seatStatus.entrySet()) {
                    String seatId = entry.getKey();
                    Showtime.SeatStatus status = entry.getValue();
                    
                    // Kiểm tra ghế đang được giữ và đã hết hạn
                    if ("holding".equals(status.getStatus()) && 
                        status.getHoldStartedAt() != null &&
                        status.getHoldStartedAt().isBefore(expiryThreshold)) {
                        
                        // Giải phóng ghế
                        status.setStatus("available");
                        status.setHoldStartedAt(null);
                        hasChanges = true;
                        
                        // Xóa khỏi cache
                        String cacheKey = showtime.getId() + "_" + seatId;
                        seatHoldCache.remove(cacheKey);
                        
                        log.info("Released expired seat hold: Showtime={}, Seat={}", 
                                showtime.getId(), seatId);
                    }
                }
            }
            
            if (hasChanges) {
                // Cập nhật số ghế trống
                updateAvailableSeats(showtime);
                showtimeRepository.save(showtime);
            }
        }
    }
    
    /**
     * Giữ ghế cho khách hàng
     */
    @Transactional
    public boolean holdSeats(String showtimeId, List<String> seatIds) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        
        LocalDateTime now = LocalDateTime.now();
        Map<String, Showtime.SeatStatus> seatStatus = showtime.getSeatStatus();
        
        if (seatStatus == null) {
            seatStatus = new ConcurrentHashMap<>();
            showtime.setSeatStatus(seatStatus);
        }
        
        // Kiểm tra tất cả ghế có sẵn không
        for (String seatId : seatIds) {
            Showtime.SeatStatus status = seatStatus.get(seatId);
            if (status != null && !"available".equals(status.getStatus())) {
                log.warn("Seat {} is not available for holding", seatId);
                return false;
            }
        }
        
        // Giữ tất cả ghế
        for (String seatId : seatIds) {
            Showtime.SeatStatus status = new Showtime.SeatStatus();
            status.setStatus("holding");
            status.setHoldStartedAt(now);
            seatStatus.put(seatId, status);
            
            // Thêm vào cache để theo dõi
            String cacheKey = showtimeId + "_" + seatId;
            seatHoldCache.put(cacheKey, now);
        }
        
        updateAvailableSeats(showtime);
        showtimeRepository.save(showtime);
        
        log.info("Held seats: Showtime={}, Seats={}", showtimeId, seatIds);
        return true;
    }
    
    /**
     * Xác nhận đặt ghế (chuyển từ holding sang booked)
     */
    @Transactional
    public boolean confirmSeatBooking(String showtimeId, List<String> seatIds, String bookingId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        
        Map<String, Showtime.SeatStatus> seatStatus = showtime.getSeatStatus();
        if (seatStatus == null) {
            return false;
        }
        
        // Kiểm tra và xác nhận tất cả ghế
        for (String seatId : seatIds) {
            Showtime.SeatStatus status = seatStatus.get(seatId);
            if (status == null || !"holding".equals(status.getStatus())) {
                log.warn("Seat {} is not in holding status", seatId);
                return false;
            }
            
            // Chuyển sang trạng thái booked
            status.setStatus("booked");
            status.setBookingId(bookingId);
            status.setHoldStartedAt(null);
            
            // Xóa khỏi cache
            String cacheKey = showtimeId + "_" + seatId;
            seatHoldCache.remove(cacheKey);
        }
        
        updateAvailableSeats(showtime);
        showtimeRepository.save(showtime);
        
        log.info("Confirmed seat booking: Showtime={}, Seats={}, BookingId={}", 
                showtimeId, seatIds, bookingId);
        return true;
    }
    
    /**
     * Hủy giữ ghế
     */
    @Transactional
    public void releaseSeatHold(String showtimeId, List<String> seatIds) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        
        Map<String, Showtime.SeatStatus> seatStatus = showtime.getSeatStatus();
        if (seatStatus == null) {
            return;
        }
        
        for (String seatId : seatIds) {
            Showtime.SeatStatus status = seatStatus.get(seatId);
            if (status != null && "holding".equals(status.getStatus())) {
                status.setStatus("available");
                status.setHoldStartedAt(null);
                
                // Xóa khỏi cache
                String cacheKey = showtimeId + "_" + seatId;
                seatHoldCache.remove(cacheKey);
            }
        }
        
        updateAvailableSeats(showtime);
        showtimeRepository.save(showtime);
        
        log.info("Released seat hold: Showtime={}, Seats={}", showtimeId, seatIds);
    }
    
    /**
     * Cập nhật số ghế trống
     */
    private void updateAvailableSeats(Showtime showtime) {
        if (showtime.getSeatStatus() == null) {
            showtime.setAvailableSeats(showtime.getTotalSeats());
            return;
        }
        
        long unavailableCount = showtime.getSeatStatus().values().stream()
                .filter(status -> !"available".equals(status.getStatus()))
                .count();
        
        showtime.setAvailableSeats(showtime.getTotalSeats() - (int) unavailableCount);
    }
    
    /**
     * Kiểm tra ghế có hết hạn không
     */
    public boolean isSeatHoldExpired(String showtimeId, String seatId) {
        String cacheKey = showtimeId + "_" + seatId;
        LocalDateTime holdTime = seatHoldCache.get(cacheKey);
        
        if (holdTime == null) {
            return true; // Không có trong cache nghĩa là đã hết hạn hoặc không được giữ
        }
        
        return holdTime.isBefore(LocalDateTime.now().minusMinutes(HOLD_EXPIRY_MINUTES));
    }
}