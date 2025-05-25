package com.cinema.config.converters; // Hoặc package bạn đã sử dụng

import com.cinema.enums.SeatState;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class SeatStateToStringConverter implements Converter<SeatState, String> {
    @Override
    public String convert(SeatState source) {
        return source == null ? null : source.getValue(); // Sử dụng giá trị chuỗi "available"
    }
}