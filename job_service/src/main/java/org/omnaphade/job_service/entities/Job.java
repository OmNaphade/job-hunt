package org.omnaphade.job_service.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs", uniqueConstraints = @UniqueConstraint(name = "uk_jobs_source_external_id", columnNames = {"source", "externalId"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    private Long companyId;

    private String location;

    private double salaryMin;

    private double salaryMax;

    private String jobType;

    private int experienceRequired;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobStatus status = JobStatus.OPEN;

    private Long createdBy;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobSource source = JobSource.RECRUITER;

    /** Provider-assigned id for imported jobs (e.g. Adzuna's job id). Null for recruiter-posted jobs. */
    private String externalId;

    /** Link back to the original listing. Null for recruiter-posted jobs. */
    private String externalUrl;

    /** Employer name for imported jobs, which have no real {@link #companyId}. Null for recruiter-posted jobs. */
    private String companyName;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = JobStatus.OPEN;
        if (source == null) source = JobSource.RECRUITER;
    }
}
