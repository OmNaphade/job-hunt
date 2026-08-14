package org.omnaphade.job_service.external;

import lombok.Builder;
import lombok.Data;

/**
 * Provider-agnostic representation of a job pulled from an external source. {@link #salaryMin} and
 * {@link #salaryMax} are intentionally boxed (unlike {@code Job}'s primitive fields) so a provider
 * that doesn't report salary can leave them {@code null} rather than lying with a fake 0.
 */
@Data
@Builder
public class ExternalJobDTO {

    private String externalId;
    private String title;
    private String description;
    private String companyName;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private String jobType;
    private String externalUrl;

}
