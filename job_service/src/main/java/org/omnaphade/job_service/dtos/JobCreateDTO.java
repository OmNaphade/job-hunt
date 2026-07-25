package org.omnaphade.job_service.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private String location;

    @PositiveOrZero
    private double minSalary;

    @PositiveOrZero
    private double maxSalary;

    private String jobType;

    @PositiveOrZero
    private int experienceRequired;

    private List<String> skills;
}