package org.omnaphade.job_service.external.aidevjobs;

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

/** Free, no-key AI/ML developer job board: https://aidevboard.com/openapi.yaml */
@Component
public class AiDevJobsClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(AiDevJobsClient.class);

    private final WebClient webClient;
    private final int limit;
    private final String query;
    private final int maxPagesPerRun;

    public AiDevJobsClient(WebClient.Builder webClientBuilder,
                            @Value("${aidevjobs.base-url:https://aidevboard.com/api/v1}") String baseUrl,
                            @Value("${aidevjobs.limit:50}") int limit,
                            @Value("${aidevjobs.query:}") String query,
                            @Value("${aidevjobs.max-pages-per-run:5}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.limit = limit;
        this.query = query;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.AI_DEV_JOBS;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    @Override
    @CircuitBreaker(name = "aiDevJobsImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        AiDevJobsSearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/jobs")
                            .queryParam("page", page)
                            .queryParam("limit", limit);
                    if (query != null && !query.isBlank()) {
                        uriBuilder.queryParam("q", query);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(AiDevJobsSearchResponse.class)
                .block();

        return AiDevJobsMapper.toExternalJobDtos(response);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling AI Dev Jobs (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
