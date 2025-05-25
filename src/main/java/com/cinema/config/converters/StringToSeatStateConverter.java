package com.cinema.config.converters; // Hoặc package bạn đã sử dụng

import com.cinema.enums.SeatState;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToSeatStateConverter implements Converter<String, SeatState> {
    @Override
    public SeatState convert(String source) {
        if (source == null) {
            return null;
        }
        // Phương thức fromValue trong SeatState enum của bạn sẽ xử lý việc này
        return SeatState.fromValue(source);
    }
}