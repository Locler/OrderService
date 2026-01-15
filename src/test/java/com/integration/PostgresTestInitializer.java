package com.integration;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("sa")
            .withPassword("sa");

    static {
        postgres.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.datasource.driver-class-name=" + postgres.getDriverClassName(),
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.liquibase.enabled=true",
                "spring.liquibase.change-log=classpath:db/changelog/changelog-master.xml",
                "user.service.base-url=http://localhost:8089"
        ).applyTo(context.getEnvironment());
    }
}
