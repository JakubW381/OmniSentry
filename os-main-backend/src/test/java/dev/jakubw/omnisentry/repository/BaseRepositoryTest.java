package dev.jakubw.omnisentry.repository;


import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@DataJpaTest
public abstract class BaseRepositoryTest {

    private static final PostgreSQLContainer postgreSQLContainer = (PostgreSQLContainer)
            new PostgreSQLContainer("postgres:17.1-alpine")
                    .withDatabaseName("main_db")
                    .withUsername("test")
                    .withPassword("test")
                    .waitingFor(Wait.forListeningPort());


    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    static{
        postgreSQLContainer.start();
    }
}
