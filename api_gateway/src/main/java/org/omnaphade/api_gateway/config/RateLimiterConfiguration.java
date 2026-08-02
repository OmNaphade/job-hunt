package org.omnaphade.api_gateway.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfiguration {

    public static final String LOGIN_BUCKET = "login";
    public static final String DEFAULT_BUCKET = "standard";

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(
            @Value("${rate-limit.login.limit:5}") int loginLimit,
            @Value("${rate-limit.login.refresh-seconds:60}") long loginRefreshSeconds,
            @Value("${rate-limit.default.limit:100}") int defaultLimit,
            @Value("${rate-limit.default.refresh-seconds:60}") long defaultRefreshSeconds) {

        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(defaultLimit)
                .limitRefreshPeriod(Duration.ofSeconds(defaultRefreshSeconds))
                .timeoutDuration(Duration.ZERO)
                .build();

        RateLimiterConfig loginConfig = RateLimiterConfig.custom()
                .limitForPeriod(loginLimit)
                .limitRefreshPeriod(Duration.ofSeconds(loginRefreshSeconds))
                .timeoutDuration(Duration.ZERO)
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        registry.addConfiguration(DEFAULT_BUCKET, defaultConfig);
        registry.addConfiguration(LOGIN_BUCKET, loginConfig);
        return registry;
    }
}
