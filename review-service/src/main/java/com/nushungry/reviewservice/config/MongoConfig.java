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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

    @Value("${mongodb.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${mongodb.ssl.ca-cert-path:/etc/documentdb/global-bundle.pem}")
    private String caCertPath;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    /**
     * 覆盖父类方法以提供带认证的 MongoDB 客户端
     * 支持 TLS/SSL 连接到 AWS DocumentDB
     */
    @Override
    public MongoClient mongoClient() {
        try {
            // 构建基础连接字符串
            String connectionString;
            if (sslEnabled) {
                connectionString = String.format(
                    "mongodb://%s:%s@%s:%d/%s?authSource=%s&authMechanism=SCRAM-SHA-1&tls=true&tlsAllowInvalidHostnames=true&retryWrites=false",
                    username, password, host, port, database, authDatabase
                );

                // 配置 SSL 上下文使用 AWS CA 证书
                MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .applyToSslSettings(builder -> {
                        builder.enabled(true);
                        try {
                            builder.context(createSSLContext());
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to create SSL context", e);
                        }
                    })
                    .build();

                return MongoClients.create(settings);
            } else {
                // 本地开发环境:无 SSL
                connectionString = String.format(
                    "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                    username, password, host, port, database, authDatabase
                );
                return MongoClients.create(connectionString);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MongoDB client", e);
        }
    }

    /**
     * 创建 SSL 上下文加载 AWS DocumentDB CA 证书
     * 支持加载包含多个证书的 PEM 文件
     */
    private SSLContext createSSLContext() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);

        try (FileInputStream fis = new FileInputStream(caCertPath)) {
            // 加载所有证书(global-bundle.pem 包含多个证书)
            int certIndex = 0;
            for (java.security.cert.Certificate cert : cf.generateCertificates(fis)) {
                ks.setCertificateEntry("documentdb-ca-" + certIndex++, cert);
            }
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }

}
