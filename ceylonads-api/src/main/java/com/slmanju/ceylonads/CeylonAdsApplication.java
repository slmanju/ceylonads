package com.slmanju.ceylonads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling backs TuitionExpiryScheduler (the first scheduled job in this project) - see
// com.slmanju.ceylonads.tuition.service.TuitionExpiryScheduler.
@SpringBootApplication
@EnableScheduling
public class CeylonAdsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CeylonAdsApplication.class, args);
    }
}
