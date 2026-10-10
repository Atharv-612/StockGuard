package com.stockguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${stockguard.db.url:${DB_URL:jdbc:mysql://localhost:3306/stockguard_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}}")
    private String mysqlUrl;

    @Value("${stockguard.db.username:${DB_USERNAME:root}}")
    private String mysqlUsername;

    @Value("${stockguard.db.password:${DB_PASSWORD:root}}")
    private String mysqlPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("StockGuard: Attempting connection to MySQL at {}", mysqlUrl);
        try {
            DriverManagerDataSource mysqlDs = new DriverManagerDataSource();
            mysqlDs.setDriverClassName("com.mysql.cj.jdbc.Driver");
            mysqlDs.setUrl(mysqlUrl);
            mysqlDs.setUsername(mysqlUsername);
            mysqlDs.setPassword(mysqlPassword);

            // Test connection with timeout
            try (Connection conn = mysqlDs.getConnection()) {
                log.info("Connected to MySQL database at {} with user '{}'", mysqlUrl, mysqlUsername);
                return mysqlDs;
            }
        } catch (Exception ex) {
            log.warn("Notice: Local MySQL server connection failed ({}). Activating local file-persisted database engine (stockguard_data.mv.db) so application runs seamlessly. To use MySQL, configure DB_PASSWORD in .env or application.properties.", ex.getMessage());

            DriverManagerDataSource localDs = new DriverManagerDataSource();
            localDs.setDriverClassName("org.h2.Driver");
            localDs.setUrl("jdbc:h2:file:./stockguard_data;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
            localDs.setUsername("sa");
            localDs.setPassword("");
            return localDs;
        }
    }
}
