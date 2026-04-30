package ru.arkhipova;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.arkhipova.configuration.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TowerApplicationTest {
    @Test
    void contextLoads() {}
}
