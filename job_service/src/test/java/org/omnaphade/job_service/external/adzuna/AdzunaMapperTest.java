package org.omnaphade.job_service.external.adzuna;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdzunaMapperTest {

    private AdzunaSearchResponse.AdzunaJobResult baseResult() {
        AdzunaSearchResponse.AdzunaJobResult result = new AdzunaSearchResponse.AdzunaJobResult();
        result.setId("12345");
        result.setTitle("Senior Java Developer");
        result.setDescription("Build and ship things.");

        AdzunaSearchResponse.AdzunaCompany company = new AdzunaSearchResponse.AdzunaCompany();
        company.setDisplayName("Example Co");
        result.setCompany(company);

        AdzunaSearchResponse.AdzunaLocation location = new AdzunaSearchResponse.AdzunaLocation();
        location.setDisplayName("Remote");
        result.setLocation(location);

        result.setRedirectUrl("https://www.adzuna.com/land/ad/12345");
        return result;
    }

    @Test
    void mapsCoreFieldsAndSalaryWhenPresent() {
        AdzunaSearchResponse.AdzunaJobResult result = baseResult();
        result.setSalaryMin(50000.0);
        result.setSalaryMax(80000.0);
        AdzunaSearchResponse response = new AdzunaSearchResponse(List.of(result), 1);

        List<ExternalJobDTO> dtos = AdzunaMapper.toExternalJobDtos(response);

        assertThat(dtos).hasSize(1);
        ExternalJobDTO dto = dtos.get(0);
        assertThat(dto.getExternalId()).isEqualTo("12345");
        assertThat(dto.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(dto.getCompanyName()).isEqualTo("Example Co");
        assertThat(dto.getLocation()).isEqualTo("Remote");
        assertThat(dto.getExternalUrl()).isEqualTo("https://www.adzuna.com/land/ad/12345");
        assertThat(dto.getSalaryMin()).isEqualTo(50000.0);
        assertThat(dto.getSalaryMax()).isEqualTo(80000.0);
    }

    @Test
    void leavesSalaryNullWhenAdzunaOmitsIt() {
        AdzunaSearchResponse.AdzunaJobResult result = baseResult();
        // salary_min/salary_max left unset, as Adzuna does for many listings.
        AdzunaSearchResponse response = new AdzunaSearchResponse(List.of(result), 1);

        ExternalJobDTO dto = AdzunaMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getSalaryMin()).isNull();
        assertThat(dto.getSalaryMax()).isNull();
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(AdzunaMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(AdzunaMapper.toExternalJobDtos(new AdzunaSearchResponse(null, 0))).isEmpty();
    }

    @Test
    void partTimeContractTimeWinsOverContractType() {
        assertThat(AdzunaMapper.toJobType("permanent", "part_time")).isEqualTo("PART_TIME");
    }

    @Test
    void contractTypeMapsToContractWhenNotPartTime() {
        assertThat(AdzunaMapper.toJobType("contract", null)).isEqualTo("CONTRACT");
        assertThat(AdzunaMapper.toJobType("contract", "full_time")).isEqualTo("CONTRACT");
    }

    @Test
    void defaultsToFullTimeWhenNoContractFieldsPresent() {
        assertThat(AdzunaMapper.toJobType(null, null)).isEqualTo("FULL_TIME");
        assertThat(AdzunaMapper.toJobType("permanent", "full_time")).isEqualTo("FULL_TIME");
    }

}
