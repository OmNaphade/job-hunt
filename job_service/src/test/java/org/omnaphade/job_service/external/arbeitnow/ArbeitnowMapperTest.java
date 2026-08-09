package org.omnaphade.job_service.external.arbeitnow;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArbeitnowMapperTest {

    private ArbeitnowSearchResponse.ArbeitnowJob job(String title, List<String> tags) {
        ArbeitnowSearchResponse.ArbeitnowJob job = new ArbeitnowSearchResponse.ArbeitnowJob();
        job.setSlug("job-" + title.toLowerCase().replace(" ", "-"));
        job.setTitle(title);
        job.setTags(tags);
        job.setCompanyName("Acme Corp");
        job.setDescription("Job description.");
        job.setUrl("https://www.arbeitnow.com/view/" + job.getSlug());
        return job;
    }

    @Test
    void keepsJobsMatchingTechKeywordsInTitle() {
        ArbeitnowSearchResponse.ArbeitnowJob techJob = job("Senior Software Developer", List.of());
        ArbeitnowSearchResponse.ArbeitnowJob nonTechJob = job("Retail Store Manager", List.of("Retail"));
        ArbeitnowSearchResponse response = new ArbeitnowSearchResponse(List.of(techJob, nonTechJob));

        List<ExternalJobDTO> dtos = ArbeitnowMapper.toExternalJobDtos(response, Set.of("developer", "engineer"));

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getTitle()).isEqualTo("Senior Software Developer");
    }

    @Test
    void keepsJobsMatchingTechKeywordsInTags() {
        ArbeitnowSearchResponse.ArbeitnowJob techJob = job("Platform Team Member", List.of("DevOps", "Cloud"));
        ArbeitnowSearchResponse response = new ArbeitnowSearchResponse(List.of(techJob));

        List<ExternalJobDTO> dtos = ArbeitnowMapper.toExternalJobDtos(response, Set.of("devops"));

        assertThat(dtos).hasSize(1);
    }

    @Test
    void emptyKeywordSetKeepsEverything() {
        ArbeitnowSearchResponse.ArbeitnowJob anyJob = job("Warehouse Associate", List.of());
        ArbeitnowSearchResponse response = new ArbeitnowSearchResponse(List.of(anyJob));

        assertThat(ArbeitnowMapper.toExternalJobDtos(response, Set.of())).hasSize(1);
    }

    @Test
    void mapsCoreFieldsForMatchedJob() {
        ArbeitnowSearchResponse.ArbeitnowJob techJob = job("Backend Engineer", List.of("Java", "Backend"));
        techJob.setLocation("Berlin, Germany");
        ArbeitnowSearchResponse response = new ArbeitnowSearchResponse(List.of(techJob));

        ExternalJobDTO dto = ArbeitnowMapper.toExternalJobDtos(response, Set.of("engineer")).get(0);

        assertThat(dto.getExternalId()).isEqualTo(techJob.getSlug());
        assertThat(dto.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(dto.getLocation()).isEqualTo("Berlin, Germany");
        assertThat(dto.getExternalUrl()).isEqualTo(techJob.getUrl());
    }

    @Test
    void locationFallsBackToRemoteFlagThenNotSpecified() {
        ArbeitnowSearchResponse.ArbeitnowJob remoteJob = job("Remote Developer", List.of());
        remoteJob.setRemote(true);
        ArbeitnowSearchResponse.ArbeitnowJob onSiteJob = job("Onsite Developer", List.of());
        onSiteJob.setRemote(false);
        ArbeitnowSearchResponse response = new ArbeitnowSearchResponse(List.of(remoteJob, onSiteJob));

        List<ExternalJobDTO> dtos = ArbeitnowMapper.toExternalJobDtos(response, Set.of("developer"));

        assertThat(dtos.get(0).getLocation()).isEqualTo("Remote");
        assertThat(dtos.get(1).getLocation()).isEqualTo("Not specified");
    }

    @Test
    void jobTypeDerivation() {
        assertThat(ArbeitnowMapper.toJobType(List.of("Full-time"))).isEqualTo("FULL_TIME");
        assertThat(ArbeitnowMapper.toJobType(List.of("Part-time"))).isEqualTo("PART_TIME");
        assertThat(ArbeitnowMapper.toJobType(List.of("Contract"))).isEqualTo("CONTRACT");
        assertThat(ArbeitnowMapper.toJobType(List.of())).isEqualTo("FULL_TIME");
        assertThat(ArbeitnowMapper.toJobType(null)).isEqualTo("FULL_TIME");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(ArbeitnowMapper.toExternalJobDtos(null, Set.of())).isEmpty();
        assertThat(ArbeitnowMapper.toExternalJobDtos(new ArbeitnowSearchResponse(null), Set.of())).isEmpty();
    }

}
