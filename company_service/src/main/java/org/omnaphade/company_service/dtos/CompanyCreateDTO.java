package org.omnaphade.company_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCreateDTO {

    @NotBlank(message = "Company name is required")
    private String name;

    private String description;
    private String website;
    private String location;
}
