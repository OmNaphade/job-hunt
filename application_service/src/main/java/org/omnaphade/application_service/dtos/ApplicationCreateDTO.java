package org.omnaphade.application_service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDTO {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @JsonIgnore
    private Long userId;
}
