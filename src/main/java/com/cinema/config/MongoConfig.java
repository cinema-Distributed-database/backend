package com.cinema.config;

import com.cinema.config.converters.StringToOldShowtimeStatusConverter;
import com.cinema.config.converters.ShowtimeStatusToStringConverter;
import com.cinema.config.converters.SeatStateToStringConverter;
import com.cinema.config.converters.StringToSeatStateConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory; // Thêm import này
import org.springframework.data.mongodb.MongoTransactionManager; // Thêm import này
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "com.cinema.repository")
@EnableTransactionManagement // Đã có
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "cinema_booking";
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new ShowtimeStatusToStringConverter());
        converters.add(new StringToOldShowtimeStatusConverter());
        converters.add(new SeatStateToStringConverter());
        converters.add(new StringToSeatStateConverter());
        return new MongoCustomConversions(converters);
    }

    // === THÊM BEAN NÀY VÀO ===
    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
    // ==========================
}