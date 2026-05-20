package com.tennis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TennisApplication {
    public static void main(String[] args) {
        SpringApplication.run(TennisApplication.class, args);
    }
}
