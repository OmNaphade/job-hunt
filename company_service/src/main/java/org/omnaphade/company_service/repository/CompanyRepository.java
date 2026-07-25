package org.omnaphade.company_service.repository;

import org.omnaphade.company_service.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
}
