package org.omnaphade.job_service.controller;

import lombok.RequiredArgsConstructor;
import org.omnaphade.job_service.service.IExternalJobImportService;
import org.omnaphade.job_service.service.ImportSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-triggerable manual import, separate from {@link JobController} since it's an operational action
 * on the import pipeline, not a CRUD operation on the Job resource itself.
 */
@RestController
@RequestMapping("/api/jobs/import")
@RequiredArgsConstructor
public class ExternalJobImportController {

    private final IExternalJobImportService importService;

    @PostMapping("/external")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportSummary> triggerImport() {
        return ResponseEntity.ok(importService.importAll());
    }

}
