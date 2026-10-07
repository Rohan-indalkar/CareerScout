package com.careerscout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CareerScoutApplication {
    public static void main(String[] args) {
        SpringApplication.run(CareerScoutApplication.class, args);
    }
}
