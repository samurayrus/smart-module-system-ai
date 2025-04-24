package ru.samurayrus.smartmodulesystemai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "data-source-master")
    @ConfigurationProperties(prefix = "app.modules.databaseworker.datasource")
    @ConditionalOnProperty(prefix = "app.modules.databaseworker", name = "enabled", havingValue = "true")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "jdbc-template-master")
    @ConditionalOnProperty(prefix = "app.modules.databaseworker", name = "enabled", havingValue = "true")
    public JdbcTemplate masterJdbcTemplate(@Qualifier("data-source-master") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
