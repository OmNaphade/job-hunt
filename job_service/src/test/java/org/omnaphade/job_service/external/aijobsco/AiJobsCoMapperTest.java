package org.omnaphade.job_service.external.aijobsco;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiJobsCoMapperTest {

    private AiJobsCoSearchResponse.AiJobsCoJob job(String title, String url) {
        AiJobsCoSearchResponse.AiJobsCoJob job = new AiJobsCoSearchResponse.AiJobsCoJob();
        job.setTitle(title);
        job.setCompany("OpenAI");
        job.setUrl(url);
        return job;
    }

    @Test
    void mapsCoreFieldsAndDerivesExternalIdFromUrlSlug() {
        AiJobsCoSearchResponse.AiJobsCoJob job = job("ML Engineer",
                "https://artificialintelligencejobs.co/jobs/ml-engineer-abc123");
        job.setLocation("San Francisco");
        job.setApplyUrl("https://boards.greenhouse.io/openai/jobs/123");
        AiJobsCoSearchResponse response = new AiJobsCoSearchResponse(List.of(job), 1);

        ExternalJobDTO dto = AiJobsCoMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("ml-engineer-abc123");
        assertThat(dto.getTitle()).isEqualTo("ML Engineer");
        assertThat(dto.getCompanyName()).isEqualTo("OpenAI");
        assertThat(dto.getLocation()).isEqualTo("San Francisco");
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://boards.greenhouse.io/openai/jobs/123");
    }

    @Test
    void externalUrlFallsBackToListingUrlWhenApplyUrlMissing() {
        AiJobsCoSearchResponse.AiJobsCoJob job = job("Engineer", "https://artificialintelligencejobs.co/jobs/engineer-xyz");
        AiJobsCoSearchResponse response = new AiJobsCoSearchResponse(List.of(job), 1);

        assertThat(AiJobsCoMapper.toExternalJobDtos(response).get(0).getExternalUrl())
                .isEqualTo("https://artificialintelligencejobs.co/jobs/engineer-xyz");
    }

    @Test
    void locationFallsBackToRemoteFlagThenNotSpecified() {
        AiJobsCoSearchResponse.AiJobsCoJob remoteJob = job("Remote Engineer", "https://x/jobs/r-1");
        remoteJob.setRemote(true);
        AiJobsCoSearchResponse.AiJobsCoJob onsiteJob = job("Onsite Engineer", "https://x/jobs/o-1");
        onsiteJob.setRemote(false);
        AiJobsCoSearchResponse response = new AiJobsCoSearchResponse(List.of(remoteJob, onsiteJob), 2);

        List<ExternalJobDTO> dtos = AiJobsCoMapper.toExternalJobDtos(response);

        assertThat(dtos.get(0).getLocation()).isEqualTo("Remote");
        assertThat(dtos.get(1).getLocation()).isEqualTo("Not specified");
    }

    @Test
    void jobsWithoutAUsableUrlAreSkipped() {
        AiJobsCoSearchResponse.AiJobsCoJob job = job("No URL", null);
        AiJobsCoSearchResponse response = new AiJobsCoSearchResponse(List.of(job), 1);

        assertThat(AiJobsCoMapper.toExternalJobDtos(response)).isEmpty();
    }

    @Test
    void externalIdDerivation() {
        assertThat(AiJobsCoMapper.toExternalId("https://x/jobs/some-slug-1234")).isEqualTo("some-slug-1234");
        assertThat(AiJobsCoMapper.toExternalId("https://x/jobs/some-slug-1234/")).isEqualTo("some-slug-1234");
        assertThat(AiJobsCoMapper.toExternalId(null)).isNull();
        assertThat(AiJobsCoMapper.toExternalId("")).isNull();
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(AiJobsCoMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(AiJobsCoMapper.toExternalJobDtos(new AiJobsCoSearchResponse(null, 0))).isEmpty();
    }

}
