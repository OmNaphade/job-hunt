package org.omnaphade.job_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the exact failure mode hit when this cache was first wired up: a {@code Page<JobResponseDTO>}
 * serialized by the same {@link Jackson2JsonRedisSerializer} {@code CacheConfig} binds {@code jobs-list}
 * etc. to (as {@code RedisCache} does on a cache write) must deserialize back into an equivalent,
 * correctly-typed {@code Page<JobResponseDTO>} (as {@code RedisCache} does on a cache hit) — not a
 * {@code LinkedHashMap}, not a Jackson {@code InvalidDefinitionException} from PageImpl's missing default
 * constructor, and not a {@code content} list of raw maps instead of real {@code JobResponseDTO}s.
 */
class CacheConfigObjectMapperTest {

    @Test
    void pageOfJobsRoundTripsThroughRedisSerializationCleanly() {
        ObjectMapper mapper = CacheConfig.buildObjectMapper();
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper,
                mapper.getTypeFactory().constructParametricType(PageImpl.class, JobResponseDTO.class));

        JobResponseDTO job = JobResponseDTO.builder()
                .id(42L)
                .title("Backend Engineer")
                .companyId(7L)
                .location("Remote")
                .jobType("FULL_TIME")
                .status("OPEN")
                .createdAt(LocalDateTime.of(2026, 8, 13, 10, 0))
                .skills(List.of("Java", "Spring"))
                .build();
        Page<JobResponseDTO> page = new PageImpl<>(List.of(job), PageRequest.of(0, 20), 1);

        byte[] serialized = serializer.serialize(page);
        Object roundTripped = serializer.deserialize(serialized);

        assertThat(roundTripped).isInstanceOf(Page.class);
        @SuppressWarnings("unchecked")
        Page<JobResponseDTO> roundTrippedPage = (Page<JobResponseDTO>) roundTripped;
        assertThat(roundTrippedPage.getTotalElements()).isEqualTo(1);
        assertThat(roundTrippedPage.getNumber()).isEqualTo(0);
        assertThat(roundTrippedPage.getSize()).isEqualTo(20);
        assertThat(roundTrippedPage.getContent()).hasSize(1);
        JobResponseDTO roundTrippedJob = roundTrippedPage.getContent().get(0);
        assertThat(roundTrippedJob).isInstanceOf(JobResponseDTO.class);
        assertThat(roundTrippedJob.getId()).isEqualTo(42L);
        assertThat(roundTrippedJob.getTitle()).isEqualTo("Backend Engineer");
        assertThat(roundTrippedJob.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 13, 10, 0));
        assertThat(roundTrippedJob.getSkills()).containsExactly("Java", "Spring");
    }

}
