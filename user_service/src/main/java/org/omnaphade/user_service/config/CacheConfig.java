package org.omnaphade.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.omnaphade.user_service.dtos.SkillDTO;
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
 * Redis-backed query cache for user_service. Only the global skills catalogue is cached (rarely changes);
 * per-user profile/skills data is deliberately left uncached to avoid staleness after profile edits. See
 * {@code UserServiceImpl#getAllSkills}/{@code #createSkill}.
 *
 * <p>{@code skills-all} is bound to a {@link Jackson2JsonRedisSerializer} for its precise
 * {@code List<SkillDTO>} return type rather than an untyped {@code GenericJackson2JsonRedisSerializer}:
 * the latter recovers a cached value's concrete type via an embedded {@code @class} JSON property, which
 * structurally doesn't work for a root-level JSON array (confirmed via {@code CacheConfigObjectMapperTest}
 * — a cache hit throws instead of returning the cached list). Binding the cache to its known type
 * sidesteps that entirely.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SKILLS_ALL = "skills-all";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = buildObjectMapper();
        RedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, SkillDTO.class));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                SKILLS_ALL, defaultConfig.entryTtl(Duration.ofMinutes(15))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }

    /** Package-private so a test can verify the List<SkillDTO> round-trip directly. */
    static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
