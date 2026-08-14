package org.omnaphade.job_service.external.findwork;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindworkMapperTest {

    private FindworkSearchResponse.FindworkJob job(String id, String role) {
        FindworkSearchResponse.FindworkJob job = new FindworkSearchResponse.FindworkJob();
        job.setId(id);
        job.setRole(role);
        job.setCompanyName("Acme Corp");
        job.setText("Job description.");
        job.setUrl("https://findwork.dev/job/" + id);
        return job;
    }

    @Test
    void mapsCoreFields() {
        FindworkSearchResponse.FindworkJob job = job("n5AvLJn", "Backend Developer");
        job.setLocation("Remote");
        job.setEmploymentType("Full Time");
        FindworkSearchResponse response = new FindworkSearchResponse(1, null, List.of(job));

        ExternalJobDTO dto = FindworkMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("n5AvLJn");
        assertThat(dto.getTitle()).isEqualTo("Backend Developer");
        assertThat(dto.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(dto.getLocation()).isEqualTo("Remote");
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://findwork.dev/job/n5AvLJn");
    }

    @Test
    void locationFallsBackToRemoteFlagThenNotSpecified() {
        FindworkSearchResponse.FindworkJob remoteJob = job("id-1", "Remote Dev");
        remoteJob.setRemote(true);
        FindworkSearchResponse.FindworkJob onsiteJob = job("id-2", "Onsite Dev");
        onsiteJob.setRemote(false);
        FindworkSearchResponse response = new FindworkSearchResponse(2, null, List.of(remoteJob, onsiteJob));

        List<ExternalJobDTO> dtos = FindworkMapper.toExternalJobDtos(response);

        assertThat(dtos.get(0).getLocation()).isEqualTo("Remote");
        assertThat(dtos.get(1).getLocation()).isEqualTo("Not specified");
    }

    @Test
    void jobTypeDerivation() {
        assertThat(FindworkMapper.toJobType("Full Time")).isEqualTo("FULL_TIME");
        assertThat(FindworkMapper.toJobType("Part Time")).isEqualTo("PART_TIME");
        assertThat(FindworkMapper.toJobType("Contract")).isEqualTo("CONTRACT");
        assertThat(FindworkMapper.toJobType(null)).isEqualTo("FULL_TIME");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(FindworkMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(FindworkMapper.toExternalJobDtos(new FindworkSearchResponse(0, null, null))).isEmpty();
    }

}
