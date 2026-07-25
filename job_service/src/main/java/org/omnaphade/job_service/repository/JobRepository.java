package org.omnaphade.job_service.repository;

import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.entities.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
       Page<Job> findByCompanyId(Long companyId, Pageable pageable);
    List<Job> findByCreatedBy(Long createdBy);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND " +
           "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(j.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))) AND " +
           "(:jobType IS NULL OR j.jobType = CAST(:jobType AS string)) AND " +
           "(:minSalary IS NULL OR j.salaryMin >= :minSalary) AND " +
           "(:maxExperience IS NULL OR j.experienceRequired <= :maxExperience)")
       Page<Job> searchJobs(@Param("status") JobStatus status,
                                           @Param("keyword") String keyword,
                                           @Param("location") String location,
                                           @Param("jobType") String jobType,
                                           @Param("minSalary") Double minSalary,
                                           @Param("maxExperience") Integer maxExperience,
                                           Pageable pageable);
}
