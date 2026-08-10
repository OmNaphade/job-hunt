package org.omnaphade.job_service.external;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobTypeNormalizerTest {

    @Test
    void detectsPartTime() {
        assertThat(JobTypeNormalizer.normalize("Part Time")).isEqualTo("PART_TIME");
        assertThat(JobTypeNormalizer.normalize("part-time")).isEqualTo("PART_TIME");
    }

    @Test
    void detectsContractSynonyms() {
        assertThat(JobTypeNormalizer.normalize("Contract")).isEqualTo("CONTRACT");
        assertThat(JobTypeNormalizer.normalize("Freelance")).isEqualTo("CONTRACT");
        assertThat(JobTypeNormalizer.normalize("Temporary")).isEqualTo("CONTRACT");
        assertThat(JobTypeNormalizer.normalize("Contractor")).isEqualTo("CONTRACT");
    }

    @Test
    void defaultsToFullTime() {
        assertThat(JobTypeNormalizer.normalize("Full Time")).isEqualTo("FULL_TIME");
        assertThat(JobTypeNormalizer.normalize("Permanent")).isEqualTo("FULL_TIME");
        assertThat(JobTypeNormalizer.normalize(null)).isEqualTo("FULL_TIME");
        assertThat(JobTypeNormalizer.normalize("")).isEqualTo("FULL_TIME");
        assertThat(JobTypeNormalizer.normalize("   ")).isEqualTo("FULL_TIME");
    }

}
