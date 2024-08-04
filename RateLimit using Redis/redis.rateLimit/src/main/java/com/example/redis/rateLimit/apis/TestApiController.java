package com.example.redis.rateLimit.apis;

import com.example.redis.rateLimit.rateLimiter.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestApiController {

    @Autowired
    private RateLimitingService rateLimitingService;

    @GetMapping("/api/testApi")
    public String testApi() {
        if(rateLimitingService.isAllowed("testKey")) {
            return "Hello from Api";
        }
        return "Rate Limited";
    }
}
