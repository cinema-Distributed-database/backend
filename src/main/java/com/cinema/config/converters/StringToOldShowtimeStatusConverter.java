package com.cinema.config.converters; // Hoặc package phù hợp

import com.cinema.enums.ShowtimeStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter // Quan trọng: Dùng khi đọc từ DB
public class StringToOldShowtimeStatusConverter implements Converter<String, ShowtimeStatus> {
    @Override
    public ShowtimeStatus convert(String source) {
        if (source == null) {
            return null;
        }
        return ShowtimeStatus.fromValue(source);
    }
}