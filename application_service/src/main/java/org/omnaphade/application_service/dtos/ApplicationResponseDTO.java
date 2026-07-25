package org.omnaphade.application_service.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDTO {

    private Long id;
    private Long jobId;
    private Long userId;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
