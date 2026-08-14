package org.omnaphade.job_service.external.jobdatalake;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobDataLakeMapperTest {

    private JobDataLakeSearchResponse.JobDataLakeJob job(String id, String title) {
        JobDataLakeSearchResponse.JobDataLakeJob job = new JobDataLakeSearchResponse.JobDataLakeJob();
        job.setId(id);
        job.setTitle(title);
        job.setCompanyName("Cargill");
        job.setUrl("https://careers.example.com/jobs/" + id);
        return job;
    }

    @Test
    void mapsCoreFields() {
        JobDataLakeSearchResponse.JobDataLakeJob job = job("abc123", "Software Engineer");
        job.setLocations(List.of("Wayzata, MN"));
        job.setEmploymentType("full_time");
        job.setSalaryMinUsd(140.0);
        job.setSalaryMaxUsd(160.0);
        JobDataLakeSearchResponse response = new JobDataLakeSearchResponse(List.of(job));

        ExternalJobDTO dto = JobDataLakeMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("abc123");
        assertThat(dto.getTitle()).isEqualTo("Software Engineer");
        assertThat(dto.getCompanyName()).isEqualTo("Cargill");
        assertThat(dto.getLocation()).isEqualTo("Wayzata, MN");
        assertThat(dto.getSalaryMin()).isEqualTo(140.0);
        assertThat(dto.getSalaryMax()).isEqualTo(160.0);
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://careers.example.com/jobs/abc123");
        assertThat(dto.getDescription()).isNull();
    }

    @Test
    void joinsMultipleLocations() {
        JobDataLakeSearchResponse.JobDataLakeJob job = job("id-1", "Engineer");
        job.setLocations(List.of("Prague, CZ", "Remote"));
        JobDataLakeSearchResponse response = new JobDataLakeSearchResponse(List.of(job));

        assertThat(JobDataLakeMapper.toExternalJobDtos(response).get(0).getLocation()).isEqualTo("Prague, CZ, Remote");
    }

    @Test
    void locationFallsBackToRemoteTypeThenNotSpecified() {
        JobDataLakeSearchResponse.JobDataLakeJob remoteJob = job("id-1", "Remote Engineer");
        remoteJob.setRemoteType("fully_remote");
        JobDataLakeSearchResponse.JobDataLakeJob onsiteJob = job("id-2", "Onsite Engineer");
        onsiteJob.setRemoteType("on_site");
        JobDataLakeSearchResponse response = new JobDataLakeSearchResponse(List.of(remoteJob, onsiteJob));

        List<ExternalJobDTO> dtos = JobDataLakeMapper.toExternalJobDtos(response);

        assertThat(dtos.get(0).getLocation()).isEqualTo("Remote");
        assertThat(dtos.get(1).getLocation()).isEqualTo("Not specified");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(JobDataLakeMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(JobDataLakeMapper.toExternalJobDtos(new JobDataLakeSearchResponse(null))).isEmpty();
    }

}
