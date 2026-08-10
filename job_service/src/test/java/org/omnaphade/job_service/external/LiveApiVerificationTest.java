package org.omnaphade.job_service.external;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.omnaphade.job_service.external.aidevjobs.AiDevJobsClient;
import org.omnaphade.job_service.external.aijobsco.AiJobsCoClient;
import org.omnaphade.job_service.external.findwork.FindworkClient;
import org.omnaphade.job_service.external.freehire.FreehireClient;
import org.omnaphade.job_service.external.jobdatalake.JobDataLakeClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One-off verification that each live provider's real JSON actually deserializes into its
 * {@code SearchResponse} DTO the way the hand-built mapper unit tests assume. Hits real network, so the
 * {@code live} tag is excluded from the default {@code mvn test} run (see the surefire config in
 * {@code pom.xml}); run it explicitly with {@code mvn test -Dtest=LiveApiVerificationTest -DexcludedGroups=}
 * after touching a provider's DTO or mapper.
 */
@Tag("live")
class LiveApiVerificationTest {

    /** Mirrors {@code WebClientConfig}'s buffer size, since this test builds its client outside Spring. */
    private final WebClient.Builder builder = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024));

    @Test
    void aiDevJobs() {
        AiDevJobsClient client = new AiDevJobsClient(builder, "https://aidevboard.com/api/v1", 5, "", 1);
        List<ExternalJobDTO> jobs = client.fetchJobs(1);
        assertThat(jobs).isNotEmpty();
        assertNonNullCoreFields(jobs.get(0));
    }

    @Test
    void artificialIntelligenceJobs() {
        AiJobsCoClient client = new AiJobsCoClient(builder, "https://artificialintelligencejobs.co", 5, "", 1);
        List<ExternalJobDTO> jobs = client.fetchJobs(1);
        assertThat(jobs).isNotEmpty();
        assertNonNullCoreFields(jobs.get(0));
    }

    @Test
    void freehire() {
        FreehireClient client = new FreehireClient(builder, "https://freehire.me/api/v1", 5, "", "", 1);
        List<ExternalJobDTO> jobs = client.fetchJobs(1);
        assertThat(jobs).isNotEmpty();
        assertNonNullCoreFields(jobs.get(0));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FINDWORK_API_KEY", matches = ".+")
    void findwork() {
        FindworkClient client = new FindworkClient(builder, "https://findwork.dev/api",
                System.getenv("FINDWORK_API_KEY"), "developer", 1);
        List<ExternalJobDTO> jobs = client.fetchJobs(1);
        assertThat(jobs).isNotEmpty();
        assertNonNullCoreFields(jobs.get(0));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JOBDATALAKE_API_KEY", matches = ".+")
    void jobDataLake() {
        JobDataLakeClient client = new JobDataLakeClient(builder, "https://api.jobdatalake.com/v1",
                System.getenv("JOBDATALAKE_API_KEY"), 5, "eng", "", 1);
        List<ExternalJobDTO> jobs = client.fetchJobs(1);
        assertThat(jobs).isNotEmpty();
        assertNonNullCoreFields(jobs.get(0));
    }

    private void assertNonNullCoreFields(ExternalJobDTO dto) {
        assertThat(dto.getExternalId()).isNotBlank();
        assertThat(dto.getTitle()).isNotBlank();
        assertThat(dto.getExternalUrl()).isNotBlank();
    }

}
