package com.cinema.config;

import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory; // Sửa import hoặc kiểu dữ liệu nếu cần
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.cinema.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    @Primary
    public SimpleMongoClientDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        // Đổi thành getDatabaseName() để sử dụng giá trị đã được inject từ properties
        return new SimpleMongoClientDatabaseFactory(mongoClient, getDatabaseName());
    }

    @Override // Thêm @Override
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, MappingMongoConverter converter) {
        // Sử dụng MongoDatabaseFactory (kiểu cha) làm kiểu tham số đầu tiên
        // Spring sẽ tự động inject bean SimpleMongoClientDatabaseFactory đã được đánh dấu @Primary vào đây.
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDatabaseFactory, converter);
        mongoTemplate.setReadPreference(ReadPreference.secondaryPreferred());
        return mongoTemplate;
    }

    @Bean
    MongoTransactionManager transactionManager(SimpleMongoClientDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}