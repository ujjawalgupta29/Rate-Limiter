package com.example.redis.rateLimit.rateLimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long REFILL_TIME = 60; // seconds
    private static final int BUCKET_CAPACITY = 5;

    public boolean isAllowed(String key) {
        String bucketKey = "bucket:" + key;
        long now = Instant.now().getEpochSecond();
        String lastRefillKey = "lastRefill:" + key;

        Long lastRefillTime = redisTemplate.opsForValue().getOperations().getExpire(lastRefillKey, TimeUnit.SECONDS);

        if (lastRefillTime == null || lastRefillTime <= 0) {
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now), REFILL_TIME, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(bucketKey, String.valueOf(BUCKET_CAPACITY));
            lastRefillTime = Long.valueOf(now);
        }

        long elapsedTime = now - lastRefillTime;
        int tokensToAdd = (int) (elapsedTime * (BUCKET_CAPACITY / REFILL_TIME));

        if (tokensToAdd > 0) {
            Integer currentTokens = Integer.valueOf(redisTemplate.opsForValue().get(bucketKey));
            int newTokens = Math.min(currentTokens + tokensToAdd, BUCKET_CAPACITY);
            redisTemplate.opsForValue().set(bucketKey, String.valueOf(newTokens));
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now), REFILL_TIME, TimeUnit.SECONDS);
        }

        Integer currentTokens = Integer.valueOf(redisTemplate.opsForValue().get(bucketKey));
        if (currentTokens > 0) {
            redisTemplate.opsForValue().decrement(bucketKey);
            return true;
        } else {
            return false;
        }
    }
}
