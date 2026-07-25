package org.omnaphade.job_service.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;

    private Long skillId;

    private String skillName;
}