package org.omnaphade.user_service.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProfileCreateDTO {

    private Long userId;

    @Size(max = 200, message = "Headline must be under 200 characters")
    private String headline;

    @Size(max = 2000, message = "Summary must be under 2000 characters")
    private String summary;

    @Min(value = 0, message = "Experience years cannot be negative")
    private int experienceYears;

    private String currentLocation;

}