package com.cinema.config.converters; // Hoặc package phù hợp

import com.cinema.enums.ShowtimeStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter // Quan trọng: Dùng khi ghi (ví dụ: tạo query)
public class ShowtimeStatusToStringConverter implements Converter<ShowtimeStatus, String> {
    @Override
    public String convert(ShowtimeStatus source) {
        return source == null ? null : source.getValue(); // Sử dụng "active" thay vì "ACTIVE"
    }
}