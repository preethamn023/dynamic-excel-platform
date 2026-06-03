package com.excel.platform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration that normalises the JDBC URL supplied by
 * Railway (or any other environment).
 *
 * Railway injects the database connection string as {@code MYSQL_URL} in the
 * form {@code mysql://user:pass@host:port/db}.  The MySQL JDBC driver requires
 * the URL to start with {@code jdbc:mysql://}.  This class detects the missing
 * prefix and adds it automatically so the application starts correctly
 * regardless of whether the URL already contains the {@code jdbc:} scheme.
 */
@Configuration
public class DataSourceConfig {

    /**
     * Raw URL from the environment.  Resolution order:
     * <ol>
     *   <li>{@code MYSQL_URL} – set by Railway's MySQL plugin</li>
     *   <li>{@code SPRING_DATASOURCE_URL} – conventional Spring override</li>
     *   <li>Hard-coded local-dev default</li>
     * </ol>
     */
    @Value("${MYSQL_URL:${spring.datasource.url:jdbc:mysql://localhost:3306/dynamic_excel_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true}}")
    private String rawUrl;

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    /**
     * Ensures the JDBC URL always starts with {@code jdbc:}.
     * <p>
     * Railway supplies {@code mysql://…}; the JDBC driver needs
     * {@code jdbc:mysql://…}.  Any URL that already carries the {@code jdbc:}
     * prefix is returned unchanged.
     */
    static String normaliseJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("DataSource URL must not be empty");
        }
        if (!url.startsWith("jdbc:")) {
            return "jdbc:" + url;
        }
        return url;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        String jdbcUrl = normaliseJdbcUrl(rawUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        return new HikariDataSource(config);
    }
}
