package com.slmanju.ceylonads.common.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("ping")
    public Map<String, String> ping() {
        return Map.of("message", "Hello from CeylonAds");
    }

    @GetMapping("/db-ping")
    public Map<String, Object> dbPing() {
        long start = System.nanoTime();
        jdbcTemplate.queryForObject("select 1", Integer.class);
        long elapsedMs =(System.nanoTime() - start) / 1_000_000;
        return Map.of("elapsedMs", elapsedMs);
    }

}
