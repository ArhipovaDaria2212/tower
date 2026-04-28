package ru.arkhipova;

import org.springframework.boot.SpringApplication;

public class TestTowerApplication {

    public static void main(String[] args) {
        SpringApplication.from(TowerApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
