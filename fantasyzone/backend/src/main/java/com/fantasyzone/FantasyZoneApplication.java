package com.fantasyzone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FantasyZoneApplication {
    public static void main(String[] args) {
        SpringApplication.run(FantasyZoneApplication.class, args);
    }
}
