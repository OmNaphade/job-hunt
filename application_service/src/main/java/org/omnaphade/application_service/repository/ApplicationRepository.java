package org.omnaphade.application_service.repository;

import org.omnaphade.application_service.entities.Application;
import org.omnaphade.application_service.entities.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserId(Long userId);
    List<Application> findByJobId(Long jobId);
    List<Application> findByJobIdAndStatus(Long jobId, ApplicationStatus status);
    Optional<Application> findByJobIdAndUserId(Long jobId, Long userId);
    boolean existsByJobIdAndUserId(Long jobId, Long userId);
}
