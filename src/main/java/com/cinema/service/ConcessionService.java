package com.cinema.service;

import com.cinema.model.Concession;
import com.cinema.repository.ConcessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcessionService {

    private final ConcessionRepository concessionRepository;

    /**
     * Lấy danh sách tất cả đồ ăn thức uống (còn hàng).
     */
    public List<Concession> getAllConcessions() {
        log.debug("Fetching all available concessions");
        return concessionRepository.findByAvailabilityTrue();
    }

    /**
     * Lấy danh sách đồ ăn thức uống theo rạp (còn hàng).
     */
    public List<Concession> getConcessionsByCinema(String cinemaId) {
        log.debug("Fetching available concessions for cinemaId: {}", cinemaId);
        // Lấy tất cả concession có sẵn nếu cinemaId là null/empty hoặc lấy theo cinemaId cụ thể
        if (cinemaId == null || cinemaId.trim().isEmpty()) {
            return concessionRepository.findByAvailabilityTrue();
        }
        return concessionRepository.findByCinemaIdsContainingAndAvailabilityTrue(cinemaId);
    }

    /**
     * Lấy chi tiết một sản phẩm đồ ăn thức uống.
     */
    public Optional<Concession> getConcessionById(String id) {
        log.debug("Fetching concession by id: {}", id);
        return concessionRepository.findById(id).filter(Concession::getAvailability);
        // Hoặc chỉ findById(id) nếu muốn admin xem cả sản phẩm không available
    }
    
    /**
     * Lấy danh sách đồ ăn thức uống theo category (còn hàng).
     */
    public List<Concession> getConcessionsByCategory(String category) {
        log.debug("Fetching available concessions by category: {}", category);
        return concessionRepository.findByCategoryAndAvailabilityTrue(category);
    }
}