package org.omnaphade.company_service.mapper;

import org.omnaphade.company_service.dtos.CompanyCreateDTO;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.entities.Company;

public class CompanyMapper {

    public static CompanyResponseDTO toDTO(Company company) {

        CompanyResponseDTO dto = new CompanyResponseDTO();

        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setDescription(company.getDescription());
        dto.setWebsite(company.getWebsite());
        dto.setLocation(company.getLocation());

        return dto;
    }

    public static Company toEntity(CompanyCreateDTO dto) {

        Company company = new Company();

        company.setName(dto.getName());
        company.setDescription(dto.getDescription());
        company.setWebsite(dto.getWebsite());
        company.setLocation(dto.getLocation());

        return company;
    }

}