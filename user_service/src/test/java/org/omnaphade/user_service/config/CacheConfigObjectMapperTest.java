package org.omnaphade.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.omnaphade.user_service.dtos.SkillDTO;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies List<SkillDTO> survives the exact same serialize/deserialize round trip {@code RedisCache}
 * performs on a cache write followed by a cache hit, using {@link Jackson2JsonRedisSerializer} bound to
 * the cache's precise type (see {@code CacheConfig} for why this is needed instead of an untyped
 * {@code GenericJackson2JsonRedisSerializer}).
 */
class CacheConfigObjectMapperTest {

    @Test
    void listOfSkillsRoundTripsThroughRedisSerializationCleanly() {
        ObjectMapper mapper = CacheConfig.buildObjectMapper();
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper,
                mapper.getTypeFactory().constructCollectionType(List.class, SkillDTO.class));

        SkillDTO skill = new SkillDTO();
        skill.setId(5L);
        skill.setName("Java");

        byte[] serialized = serializer.serialize(List.of(skill));
        Object roundTripped = serializer.deserialize(serialized);

        assertThat(roundTripped).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<SkillDTO> roundTrippedList = (List<SkillDTO>) roundTripped;
        assertThat(roundTrippedList).hasSize(1);
        assertThat(roundTrippedList.get(0)).isInstanceOf(SkillDTO.class);
        assertThat(roundTrippedList.get(0).getName()).isEqualTo("Java");
    }

}
