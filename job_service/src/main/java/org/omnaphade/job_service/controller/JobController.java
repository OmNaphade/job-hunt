package org.omnaphade.job_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.omnaphade.job_service.dtos.JobCreateDTO;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.omnaphade.job_service.entities.JobStatus;
import org.omnaphade.job_service.service.IJobService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final IJobService jobService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobResponseDTO> createJob(
            @Valid @RequestBody JobCreateDTO dto,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(dto, userId));
    }

    @GetMapping
    public ResponseEntity<Page<JobResponseDTO>> getAllJobs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(jobService.getAllJobs(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<JobResponseDTO>> searchJobs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "jobType", required = false) String jobType,
            @RequestParam(name = "minSalary", required = false) Double minSalary,
            @RequestParam(name = "maxExperience", required = false) Integer maxExperience,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(jobService.searchJobs(
                keyword,
                location,
                jobType,
                minSalary,
                maxExperience,
                page,
                size,
                sortBy,
                sortDir
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getJobById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<JobResponseDTO>> getJobsByCompany(
            @PathVariable("companyId") Long companyId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(jobService.getJobsByCompany(companyId, page, size, sortBy, sortDir));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobResponseDTO> updateJob(
            @PathVariable("id") Long id,
            @Valid @RequestBody JobCreateDTO dto,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(jobService.updateJob(id, dto, userId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobResponseDTO> updateJobStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") JobStatus status,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(jobService.updateJobStatus(id, status, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Void> deleteJob(
            @PathVariable("id") Long id,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        jobService.deleteJob(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/save")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<Void> saveJob(@PathVariable("id") Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        jobService.saveJob(userId, id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/save")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<Void> unsaveJob(@PathVariable("id") Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        jobService.unsaveJob(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/saved")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<Page<JobResponseDTO>> getSavedJobs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(jobService.getSavedJobs(userId, page, size));
    }

    @GetMapping("/saved/ids")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<List<Long>> getSavedJobIds(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(jobService.getSavedJobIds(userId));
    }
}
