package org.omnaphade.job_service.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed query cache for job_service's public read endpoints (listing/search/by-id/by-company).
 * Per-user endpoints (saved jobs) are deliberately left uncached — see {@code JobServiceImpl} for the
 * {@code @Cacheable} usages this config backs, and the plan's cache-name/TTL table for the full picture.
 *
 * <p>Each named cache is bound to a {@link Jackson2JsonRedisSerializer} for its own precise return type
 * rather than one shared, untyped {@code GenericJackson2JsonRedisSerializer}. That's deliberate:
 * {@code GenericJackson2JsonRedisSerializer} relies on embedding a {@code @class} JSON <em>property</em>
 * to recover a cached value's concrete type, which structurally doesn't work for a root-level
 * {@code Page<T>}/list value (confirmed via {@code CacheConfigObjectMapperTest}, reproducing exactly what
 * {@link RedisCacheManager} hits on a real cache hit). Binding each cache to its own known type sidesteps
 * that entirely — the type is fixed at construction, not recovered from the payload.
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
        ObjectMapper objectMapper = buildObjectMapper();
        JavaType pageOfJobsType = objectMapper.getTypeFactory().constructParametricType(PageImpl.class, JobResponseDTO.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                JOBS_LIST, withValueType(defaultConfig, objectMapper, pageOfJobsType).entryTtl(Duration.ofMinutes(2)),
                JOBS_SEARCH, withValueType(defaultConfig, objectMapper, pageOfJobsType).entryTtl(Duration.ofMinutes(2)),
                JOBS_BY_COMPANY, withValueType(defaultConfig, objectMapper, pageOfJobsType).entryTtl(Duration.ofMinutes(2)),
                JOBS_BY_ID, withValueType(defaultConfig, objectMapper, objectMapper.constructType(JobResponseDTO.class))
                        .entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }

    private static RedisCacheConfiguration withValueType(RedisCacheConfiguration base, ObjectMapper objectMapper, JavaType type) {
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, type);
        return base.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    /** Package-private so {@code CacheConfigObjectMapperTest} can verify the Page<T> round-trip directly. */
    static ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // PageImpl has no no-arg constructor or Jackson-visible creator — see PageImplDeserializer.
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(PageImpl.class, new PageImplDeserializer());
        objectMapper.registerModule(pageModule);
        return objectMapper;
    }

}
