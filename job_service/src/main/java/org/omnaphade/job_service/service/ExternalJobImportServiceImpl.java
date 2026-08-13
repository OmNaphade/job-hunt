package org.omnaphade.job_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.job_service.config.CacheConfig;
import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.ExternalJobMapper;
import org.omnaphade.job_service.external.ExternalJobProvider;
import org.omnaphade.job_service.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates external job import: for every configured {@link ExternalJobProvider}, pages through its
 * results and upserts each job by (source, externalId). Knows nothing about HTTP or any specific
 * provider's field quirks — that's {@code AdzunaClient}/{@code AdzunaMapper}'s job.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExternalJobImportServiceImpl implements IExternalJobImportService {

    private static final Logger log = LoggerFactory.getLogger(ExternalJobImportServiceImpl.class);

    private final List<ExternalJobProvider> providers;
    private final JobRepository jobRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.JOBS_LIST, allEntries = true),
            @CacheEvict(value = CacheConfig.JOBS_SEARCH, allEntries = true),
            @CacheEvict(value = CacheConfig.JOBS_BY_COMPANY, allEntries = true)
    })
    public ImportSummary importAll() {
        int fetched = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (ExternalJobProvider provider : providers) {
            List<ExternalJobDTO> jobs = fetchAllPages(provider);
            fetched += jobs.size();

            for (ExternalJobDTO dto : jobs) {
                if (dto.getExternalId() == null || dto.getExternalId().isBlank()
                        || dto.getTitle() == null || dto.getTitle().isBlank()) {
                    skipped++;
                    continue;
                }

                Optional<Job> existing = jobRepository.findBySourceAndExternalId(provider.getSource(), dto.getExternalId());
                if (existing.isPresent()) {
                    Job job = existing.get();
                    ExternalJobMapper.updateExisting(job, dto);
                    jobRepository.save(job);
                    updated++;
                } else {
                    jobRepository.save(ExternalJobMapper.toNewEntity(dto, provider.getSource()));
                    created++;
                }
            }
        }

        log.info("External job import complete: fetched={}, created={}, updated={}, skipped={}", fetched, created, updated, skipped);
        return new ImportSummary(fetched, created, updated, skipped);
    }

    private List<ExternalJobDTO> fetchAllPages(ExternalJobProvider provider) {
        List<ExternalJobDTO> all = new ArrayList<>();
        for (int page = 1; page <= provider.getMaxPagesPerRun(); page++) {
            List<ExternalJobDTO> pageResults = provider.fetchJobs(page);
            if (pageResults == null || pageResults.isEmpty()) {
                break;
            }
            all.addAll(pageResults);
        }
        return all;
    }

}
