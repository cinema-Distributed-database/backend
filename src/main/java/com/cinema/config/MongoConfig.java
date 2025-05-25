package com.cinema.config;

import com.cinema.config.converters.StringToOldShowtimeStatusConverter;
import com.cinema.config.converters.ShowtimeStatusToStringConverter;
// Thêm import cho các converter mới của SeatState
import com.cinema.config.converters.SeatStateToStringConverter;
import com.cinema.config.converters.StringToSeatStateConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "com.cinema.repository")
@EnableTransactionManagement
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "cinema_booking";
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        // Converters cho ShowtimeStatus (đã có từ trước)
        converters.add(new ShowtimeStatusToStringConverter());
        converters.add(new StringToOldShowtimeStatusConverter()); // Đảm bảo tên class này đúng với file bạn đã tạo

        // Thêm converters mới cho SeatState
        converters.add(new SeatStateToStringConverter());
        converters.add(new StringToSeatStateConverter());

        // Thêm các converters khác nếu cần
        return new MongoCustomConversions(converters);
    }
}