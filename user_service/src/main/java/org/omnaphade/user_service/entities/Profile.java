package org.omnaphade.user_service.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String headline;

    @Column(length = 2000)
    private String summary;

    private int experienceYears;

    private String currentLocation;

    private LocalDateTime createdAt;

}