package com.ia.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IaAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IaAggregatorApplication.class, args);
    }
}
