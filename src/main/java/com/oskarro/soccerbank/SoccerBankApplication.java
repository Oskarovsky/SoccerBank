package com.oskarro.soccerbank;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class SoccerBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoccerBankApplication.class, args);
    }

}
