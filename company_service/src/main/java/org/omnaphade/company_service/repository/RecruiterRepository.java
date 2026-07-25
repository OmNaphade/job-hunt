package org.omnaphade.company_service.repository;

import org.omnaphade.company_service.entities.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    List<Recruiter> findByCompanyId(Long companyId);
    Optional<Recruiter> findByUserId(Long userId);
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);
}
