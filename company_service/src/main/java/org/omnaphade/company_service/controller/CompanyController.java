package org.omnaphade.company_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.omnaphade.company_service.dtos.CompanyCreateDTO;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.dtos.RecruiterDTO;
import org.omnaphade.company_service.service.ICompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final ICompanyService companyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<CompanyResponseDTO> createCompany(@Valid @RequestBody CompanyCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(dto));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponseDTO>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<CompanyResponseDTO> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyCreateDTO dto) {
        return ResponseEntity.ok(companyService.updateCompany(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{companyId}/recruiters")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<RecruiterDTO> addRecruiter(
            @PathVariable Long companyId,
            @Valid @RequestBody RecruiterDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.addRecruiter(companyId, dto));
    }

    @GetMapping("/{companyId}/recruiters")
    public ResponseEntity<List<RecruiterDTO>> getRecruiters(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyService.getRecruiters(companyId));
    }

    @DeleteMapping("/{companyId}/recruiters/{recruiterId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Void> removeRecruiter(
            @PathVariable Long companyId,
            @PathVariable Long recruiterId) {
        companyService.removeRecruiter(companyId, recruiterId);
        return ResponseEntity.noContent().build();
    }
}
