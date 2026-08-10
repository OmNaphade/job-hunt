package org.omnaphade.job_service.external.aijobsco;

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

/** Free, no-key job board for AI/ML roles: https://artificialintelligencejobs.co/developers */
@Component
public class AiJobsCoClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(AiJobsCoClient.class);

    private final WebClient webClient;
    private final int limit;
    private final String query;
    private final int maxPagesPerRun;

    public AiJobsCoClient(WebClient.Builder webClientBuilder,
                           @Value("${aijobsco.base-url:https://artificialintelligencejobs.co}") String baseUrl,
                           @Value("${aijobsco.limit:30}") int limit,
                           @Value("${aijobsco.query:}") String query,
                           @Value("${aijobsco.max-pages-per-run:5}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.limit = limit;
        this.query = query;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.ARTIFICIAL_INTELLIGENCE_JOBS;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    /** {@code page} is translated to an {@code offset} since this API has no native page number. */
    @Override
    @CircuitBreaker(name = "aiJobsCoImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        int offset = Math.max(0, page - 1) * limit;

        AiJobsCoSearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/jobs")
                            .queryParam("limit", limit)
                            .queryParam("offset", offset);
                    if (query != null && !query.isBlank()) {
                        uriBuilder.queryParam("q", query);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(AiJobsCoSearchResponse.class)
                .block();

        return AiJobsCoMapper.toExternalJobDtos(response);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling Artificial Intelligence Jobs (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
