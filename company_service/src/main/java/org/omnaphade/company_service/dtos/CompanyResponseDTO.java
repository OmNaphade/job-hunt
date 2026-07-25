package org.omnaphade.company_service.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String website;
    private String location;
    private LocalDateTime createdAt;
}
