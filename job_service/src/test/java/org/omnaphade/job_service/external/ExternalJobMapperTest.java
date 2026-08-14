package org.omnaphade.job_service.external;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.entities.JobSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobMapperTest {

    @Test
    void toNewEntityStripsHtmlFromDescription() {
        ExternalJobDTO dto = ExternalJobDTO.builder()
                .externalId("123")
                .title("Senior DevOps Engineer")
                .description("<p>Are you a <strong>talented</strong> DevOps engineer?</p>"
                        + "<!-- notionvc: 9b231391-b97c-4ae2-b256-ce17bc98e9a7 -->"
                        + "<ul><li>4+ years experience</li></ul>")
                .companyName("Lemon.io")
                .location("Remote")
                .jobType("FULL_TIME")
                .externalUrl("https://remotive.com/remote-jobs/123")
                .build();

        Job job = ExternalJobMapper.toNewEntity(dto, JobSource.REMOTIVE);

        assertThat(job.getDescription())
                .isEqualTo("Are you a talented DevOps engineer? 4+ years experience")
                .doesNotContain("<p>", "<strong>", "<!--", "notionvc");
    }

    @Test
    void updateExistingStripsHtmlFromDescription() {
        Job job = Job.builder().title("Old Title").description("old").build();
        ExternalJobDTO dto = ExternalJobDTO.builder()
                .title("New Title")
                .description("<div>Updated <em>description</em></div>")
                .build();

        ExternalJobMapper.updateExisting(job, dto);

        assertThat(job.getDescription()).isEqualTo("Updated description");
    }

}
