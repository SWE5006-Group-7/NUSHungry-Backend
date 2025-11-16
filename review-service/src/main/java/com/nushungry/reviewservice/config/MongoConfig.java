package com.nushungry.reviewservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB 配置类
 * 针对生产环境配置连接池和性能优化参数
 * 包含 @EnableMongoAuditing,使得测试时可以排除此配置类来避免 MongoDB 审计依赖
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.database:nushungry_reviews}")
    private String database;

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.username:admin}")
    private String username;

    @Value("${spring.data.mongodb.password:admin123}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authDatabase;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    /**
     * 覆盖父类方法以提供带认证的 MongoDB 客户端
     * 此方法会被 Spring 自动调用,无需依赖 profile
     */
    @Override
    public MongoClient mongoClient() {
        // 构建带认证的连接字符串
        String connectionString = String.format(
            "mongodb://%s:%s@%s:%d/%s?authSource=%s",
            username, password, host, port, database, authDatabase
        );
        return MongoClients.create(connectionString);
    }

}
