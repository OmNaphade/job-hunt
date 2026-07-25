package org.omnaphade.company_service.service;

import org.omnaphade.company_service.dtos.CompanyCreateDTO;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.dtos.RecruiterDTO;

import java.util.List;

public interface ICompanyService {
    CompanyResponseDTO createCompany(CompanyCreateDTO dto);
    CompanyResponseDTO getCompanyById(Long id);
    List<CompanyResponseDTO> getAllCompanies();
    CompanyResponseDTO updateCompany(Long id, CompanyCreateDTO dto);
    void deleteCompany(Long id);
    RecruiterDTO addRecruiter(Long companyId, RecruiterDTO dto);
    List<RecruiterDTO> getRecruiters(Long companyId);
    void removeRecruiter(Long companyId, Long recruiterId);
}
