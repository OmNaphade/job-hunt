package org.omnaphade.job_service.external;

import org.omnaphade.job_service.entities.JobSource;

import java.util.List;

/**
 * A source of external job listings. Adding a new provider is a new implementation of this interface
 * (e.g. a second {@code @Component} for another job board) — {@code ExternalJobImportServiceImpl} never
 * needs to change, since Spring autowires every {@code ExternalJobProvider} bean automatically.
 */
public interface ExternalJobProvider {

    JobSource getSource();

    /**
     * Fetches one page of results (1-indexed, matching Adzuna's own paging convention). Implementations
     * must never throw and must never return {@code null} — on any failure, return an empty list so the
     * importer can safely stop paging.
     */
    List<ExternalJobDTO> fetchJobs(int page);

}
