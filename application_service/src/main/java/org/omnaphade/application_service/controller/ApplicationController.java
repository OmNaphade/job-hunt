package org.omnaphade.application_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.omnaphade.application_service.dtos.ApplicationCreateDTO;
import org.omnaphade.application_service.dtos.ApplicationResponseDTO;
import org.omnaphade.application_service.entities.ApplicationStatus;
import org.omnaphade.application_service.service.IApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final IApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<ApplicationResponseDTO> applyForJob(
            @Valid @RequestBody ApplicationCreateDTO dto,
            Authentication auth) {
        dto.setUserId(Long.parseLong(auth.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.applyForJob(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDTO> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(applicationService.getApplicationsByUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> getApplicationsByJob(@RequestParam Long jobId) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApplicationResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        applicationService.withdrawApplication(id, userId);
        return ResponseEntity.noContent().build();
    }
}
