package org.omnaphade.company_service.mapper;

import org.omnaphade.company_service.dtos.RecruiterDTO;
import org.omnaphade.company_service.entities.Recruiter;

public class RecruiterMapper {

    public static RecruiterDTO toDTO(Recruiter recruiter) {

        RecruiterDTO dto = new RecruiterDTO();

        dto.setId(recruiter.getId());
        dto.setUserId(recruiter.getUserId());
        dto.setCompanyId(recruiter.getCompanyId());
        dto.setDesignation(recruiter.getDesignation());
        dto.setVerified(recruiter.isVerified());

        return dto;
    }

    public static Recruiter toEntity(RecruiterDTO dto) {

        Recruiter recruiter = new Recruiter();

        recruiter.setUserId(dto.getUserId());
        recruiter.setCompanyId(dto.getCompanyId());
        recruiter.setDesignation(dto.getDesignation());
        recruiter.setVerified(dto.isVerified());

        return recruiter;
    }

}