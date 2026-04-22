package com.openroof.openroof.config;

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "9m", defaultLockAtLeastFor = "1m")
public class SchedulerLockConfig {

    @Bean
    public JdbcTemplateLockProvider lockProvider(DataSource ds) {
        return new JdbcTemplateLockProvider(new JdbcTemplate(ds));
    }
}