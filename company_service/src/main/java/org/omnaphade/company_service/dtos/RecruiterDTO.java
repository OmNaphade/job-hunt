package org.omnaphade.company_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterDTO {

    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long companyId;

    @NotBlank(message = "Designation is required")
    private String designation;

    private boolean verified;
}
