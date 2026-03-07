package com.vetplatform.reviewextractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReviewExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewExtractorApplication.class, args);
    }
}
