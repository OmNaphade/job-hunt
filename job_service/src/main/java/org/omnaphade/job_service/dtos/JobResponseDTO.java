package org.omnaphade.job_service.dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponseDTO {

    private Long id;
    private String title;
    private String description;
    private Long companyId;
    private String location;
    private double minSalary;
    private double maxSalary;
    private String jobType;
    private int experienceRequired;
    private String status;
    private LocalDateTime createdAt;
    private List<String> skills;
}