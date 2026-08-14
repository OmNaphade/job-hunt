package org.omnaphade.company_service.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.dtos.RecruiterDTO;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis-backed query cache for company_service's public read endpoints. See {@code CompanyServiceImpl}
 * for the {@code @Cacheable} usages this config backs.
 *
 * <p>Each named cache is bound to a {@link Jackson2JsonRedisSerializer} for its own precise return type
 * rather than one shared, untyped {@code GenericJackson2JsonRedisSerializer}. That's deliberate:
 * {@code GenericJackson2JsonRedisSerializer} relies on embedding a {@code @class} JSON <em>property</em>
 * to recover a cached value's concrete type, but a root-level {@code List<T>} serializes as a bare JSON
 * array — which structurally has no place to put that property — so a cached list can never be read back
 * (confirmed via {@code CacheConfigObjectMapperTest}, reproducing exactly what {@link RedisCacheManager}
 * hits on a real cache hit). Binding each cache to its own known type sidesteps the problem entirely: the
 * type is fixed at construction, not recovered from the payload.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String COMPANIES_LIST = "companies-list";
    public static final String COMPANIES_BY_ID = "companies-by-id";
    public static final String COMPANY_RECRUITERS = "company-recruiters";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = buildObjectMapper();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                COMPANIES_LIST, withValueType(defaultConfig, objectMapper,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, CompanyResponseDTO.class))
                        .entryTtl(Duration.ofMinutes(5)),
                COMPANIES_BY_ID, withValueType(defaultConfig, objectMapper, objectMapper.constructType(CompanyResponseDTO.class))
                        .entryTtl(Duration.ofMinutes(10)),
                COMPANY_RECRUITERS, withValueType(defaultConfig, objectMapper,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, RecruiterDTO.class))
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

    /** Package-private so a test can verify each cache's round-trip directly. */
    static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
