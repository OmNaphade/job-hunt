package org.omnaphade.company_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.company_service.dtos.CompanyCreateDTO;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.dtos.RecruiterDTO;
import org.omnaphade.company_service.entities.Company;
import org.omnaphade.company_service.entities.Recruiter;
import org.omnaphade.company_service.exception.DuplicateResourceException;
import org.omnaphade.company_service.exception.ResourceNotFoundException;
import org.omnaphade.company_service.mapper.CompanyMapper;
import org.omnaphade.company_service.mapper.RecruiterMapper;
import org.omnaphade.company_service.repository.CompanyRepository;
import org.omnaphade.company_service.repository.RecruiterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements ICompanyService {

    private final CompanyRepository companyRepository;
    private final RecruiterRepository recruiterRepository;

    @Override
    public CompanyResponseDTO createCompany(CompanyCreateDTO dto) {
        if (companyRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Company already exists: " + dto.getName());
        }
        Company company = CompanyMapper.toEntity(dto);
        return CompanyMapper.toDTO(companyRepository.save(company));
    }

    @Override
    public CompanyResponseDTO getCompanyById(Long id) {
        return CompanyMapper.toDTO(findById(id));
    }

    @Override
    public List<CompanyResponseDTO> getAllCompanies() {
        return companyRepository.findAll().stream().map(CompanyMapper::toDTO).toList();
    }

    @Override
    public CompanyResponseDTO updateCompany(Long id, CompanyCreateDTO dto) {
        Company company = findById(id);
        company.setName(dto.getName());
        company.setDescription(dto.getDescription());
        company.setWebsite(dto.getWebsite());
        company.setLocation(dto.getLocation());
        return CompanyMapper.toDTO(companyRepository.save(company));
    }

    @Override
    public void deleteCompany(Long id) {
        Company company = findById(id);
        companyRepository.delete(company);
    }

    @Override
    public RecruiterDTO addRecruiter(Long companyId, RecruiterDTO dto) {
        findById(companyId); // ensure company exists
        if (recruiterRepository.existsByUserIdAndCompanyId(dto.getUserId(), companyId)) {
            throw new DuplicateResourceException("User is already a recruiter for this company");
        }
        Recruiter recruiter = RecruiterMapper.toEntity(dto);
        recruiter.setCompanyId(companyId);
        return RecruiterMapper.toDTO(recruiterRepository.save(recruiter));
    }

    @Override
    public List<RecruiterDTO> getRecruiters(Long companyId) {
        findById(companyId); // ensure company exists
        return recruiterRepository.findByCompanyId(companyId).stream()
                .map(RecruiterMapper::toDTO).toList();
    }

    @Override
    public void removeRecruiter(Long companyId, Long recruiterId) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));
        if (!recruiter.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Recruiter does not belong to this company");
        }
        recruiterRepository.delete(recruiter);
    }

    private Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }
}
