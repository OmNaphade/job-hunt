package org.omnaphade.job_service.external.jobdatalake;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.omnaphade.job_service.entities.JobSource;
import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.ExternalJobProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Large-scale job aggregator, requires a free API key (X-API-Key header): https://www.jobdatalake.com/docs
 * Scoped to IT/tech by default via "job-function=eng", mirroring how {@code AdzunaClient} defaults to the
 * "it-jobs" category.
 */
@Component
public class JobDataLakeClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(JobDataLakeClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final int perPage;
    private final String jobFunction;
    private final String keyword;
    private final int maxPagesPerRun;

    public JobDataLakeClient(WebClient.Builder webClientBuilder,
                              @Value("${jobdatalake.base-url:https://api.jobdatalake.com/v1}") String baseUrl,
                              @Value("${jobdatalake.api-key:}") String apiKey,
                              @Value("${jobdatalake.per-page:50}") int perPage,
                              @Value("${jobdatalake.job-function:eng}") String jobFunction,
                              @Value("${jobdatalake.keyword:}") String keyword,
                              @Value("${jobdatalake.max-pages-per-run:5}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.perPage = perPage;
        this.jobFunction = jobFunction;
        this.keyword = keyword;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.JOBDATALAKE;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    @Override
    @CircuitBreaker(name = "jobDataLakeImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        if (apiKey.isBlank()) {
            log.warn("JobDataLake credentials not configured (jobdatalake.api-key blank); skipping import.");
            return List.of();
        }

        JobDataLakeSearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/jobs")
                            .queryParam("page", page)
                            .queryParam("limit", perPage);
                    if (!jobFunction.isBlank()) {
                        uriBuilder.queryParam("job_function", jobFunction);
                    }
                    if (!keyword.isBlank()) {
                        uriBuilder.queryParam("q", keyword);
                    }
                    return uriBuilder.build();
                })
                .header("X-API-Key", apiKey)
                .retrieve()
                .bodyToMono(JobDataLakeSearchResponse.class)
                .block();

        return JobDataLakeMapper.toExternalJobDtos(response);
    }

    /** Mirrors the guarded method's params + a trailing Throwable, per Resilience4j's fallback contract. */
    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling JobDataLake (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
