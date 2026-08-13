package org.omnaphade.job_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed query cache for job_service's public read endpoints (listing/search/by-id/by-company).
 * Per-user endpoints (saved jobs) are deliberately left uncached — see {@code JobServiceImpl} for the
 * {@code @Cacheable} usages this config backs, and the plan's cache-name/TTL table for the full picture.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String JOBS_LIST = "jobs-list";
    public static final String JOBS_SEARCH = "jobs-search";
    public static final String JOBS_BY_COMPANY = "jobs-by-company";
    public static final String JOBS_BY_ID = "jobs-by-id";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                JOBS_LIST, defaultConfig.entryTtl(Duration.ofMinutes(2)),
                JOBS_SEARCH, defaultConfig.entryTtl(Duration.ofMinutes(2)),
                JOBS_BY_COMPANY, defaultConfig.entryTtl(Duration.ofMinutes(2)),
                JOBS_BY_ID, defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }

}
