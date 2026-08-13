package org.omnaphade.company_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.dtos.RecruiterDTO;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies each named cache's value type survives the exact same serialize/deserialize round trip
 * {@code RedisCache} performs on a write followed by a hit. A plain {@code List<T>} bound to a shared,
 * untyped {@code GenericJackson2JsonRedisSerializer} cannot do this (a root-level JSON array has no place
 * to embed the {@code @class} property that serializer relies on) — this is why each cache is instead
 * bound to its own {@link Jackson2JsonRedisSerializer} with a statically-known target type.
 */
class CacheConfigObjectMapperTest {

    private final ObjectMapper objectMapper = CacheConfig.buildObjectMapper();

    @Test
    void listOfCompaniesRoundTripsThroughRedisSerializationCleanly() {
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CompanyResponseDTO.class));

        List<CompanyResponseDTO> companies = List.of(
                CompanyResponseDTO.builder()
                        .id(1L)
                        .name("Acme Corp")
                        .location("Remote")
                        .createdAt(LocalDateTime.of(2026, 8, 13, 10, 0))
                        .build()
        );

        byte[] serialized = serializer.serialize(companies);
        Object roundTripped = serializer.deserialize(serialized);

        assertThat(roundTripped).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<CompanyResponseDTO> roundTrippedList = (List<CompanyResponseDTO>) roundTripped;
        assertThat(roundTrippedList).hasSize(1);
        assertThat(roundTrippedList.get(0).getName()).isEqualTo("Acme Corp");
        assertThat(roundTrippedList.get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 13, 10, 0));
    }

    @Test
    void singleCompanyRoundTripsThroughRedisSerializationCleanly() {
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, objectMapper.constructType(CompanyResponseDTO.class));

        CompanyResponseDTO company = CompanyResponseDTO.builder().id(9L).name("Beta LLC").build();

        byte[] serialized = serializer.serialize(company);
        Object roundTripped = serializer.deserialize(serialized);

        assertThat(roundTripped).isInstanceOf(CompanyResponseDTO.class);
        assertThat(((CompanyResponseDTO) roundTripped).getName()).isEqualTo("Beta LLC");
    }

    @Test
    void listOfRecruitersRoundTripsThroughRedisSerializationCleanly() {
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, RecruiterDTO.class));

        List<RecruiterDTO> recruiters = List.of(RecruiterDTO.builder().userId(3L).build());

        byte[] serialized = serializer.serialize(recruiters);
        Object roundTripped = serializer.deserialize(serialized);

        assertThat(roundTripped).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<RecruiterDTO> roundTrippedList = (List<RecruiterDTO>) roundTripped;
        assertThat(roundTrippedList).hasSize(1);
        assertThat(roundTrippedList.get(0).getUserId()).isEqualTo(3L);
    }

}
