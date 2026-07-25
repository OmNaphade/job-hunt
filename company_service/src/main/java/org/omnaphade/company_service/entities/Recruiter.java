package org.omnaphade.company_service.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recruiters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recruiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long companyId;

    private String designation;

    private boolean verified;
}
