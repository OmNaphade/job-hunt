package org.omnaphade.job_service.external.freehire;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FreehireMapperTest {

    private FreehireSearchResponse.FreehireJob job(String publicSlug, String title) {
        FreehireSearchResponse.FreehireJob job = new FreehireSearchResponse.FreehireJob();
        job.setPublicSlug(publicSlug);
        job.setTitle(title);
        job.setCompany("PriceLabs");
        job.setDescription("Job description.");
        job.setUrl("https://freehire.me/jobs/" + publicSlug);
        return job;
    }

    @Test
    void mapsCoreFieldsUsingPublicSlugAsExternalId() {
        FreehireSearchResponse.FreehireJob job = job("solutions-consultant-abc", "Solutions Consultant");
        job.setLocation("Chicago, IL");
        job.setExternalId("pricelabs:SBDjbemHcRf7");
        FreehireSearchResponse response = new FreehireSearchResponse(List.of(job));

        ExternalJobDTO dto = FreehireMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("solutions-consultant-abc");
        assertThat(dto.getTitle()).isEqualTo("Solutions Consultant");
        assertThat(dto.getCompanyName()).isEqualTo("PriceLabs");
        assertThat(dto.getLocation()).isEqualTo("Chicago, IL");
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://freehire.me/jobs/solutions-consultant-abc");
    }

    @Test
    void locationFallsBackToWorkModeThenNotSpecified() {
        FreehireSearchResponse.FreehireJob remoteJob = job("r-1", "Remote Engineer");
        remoteJob.setWorkMode("remote");
        FreehireSearchResponse.FreehireJob onsiteJob = job("o-1", "Onsite Engineer");
        onsiteJob.setWorkMode("onsite");
        FreehireSearchResponse response = new FreehireSearchResponse(List.of(remoteJob, onsiteJob));

        List<ExternalJobDTO> dtos = FreehireMapper.toExternalJobDtos(response);

        assertThat(dtos.get(0).getLocation()).isEqualTo("Remote");
        assertThat(dtos.get(1).getLocation()).isEqualTo("Not specified");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(FreehireMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(FreehireMapper.toExternalJobDtos(new FreehireSearchResponse(null))).isEmpty();
    }

}
