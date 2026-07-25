package org.omnaphade.user_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProfileResponseDTO {

    private Long id;
    private Long userId;
    private String headline;
    private String summary;
    private int experienceYears;
    private String currentLocation;

}
