package org.omnaphade.job_service.external.adzuna;

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

@Component
public class AdzunaClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(AdzunaClient.class);

    private final WebClient webClient;
    private final String appId;
    private final String appKey;
    private final String country;
    private final int resultsPerPage;

    public AdzunaClient(WebClient.Builder webClientBuilder,
                         @Value("${adzuna.base-url:https://api.adzuna.com/v1/api}") String baseUrl,
                         @Value("${adzuna.app-id:}") String appId,
                         @Value("${adzuna.app-key:}") String appKey,
                         @Value("${adzuna.country:us}") String country,
                         @Value("${adzuna.results-per-page:20}") int resultsPerPage) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.appId = appId;
        this.appKey = appKey;
        this.country = country;
        this.resultsPerPage = resultsPerPage;
    }

    @Override
    public JobSource getSource() {
        return JobSource.ADZUNA;
    }

    @Override
    @CircuitBreaker(name = "adzunaImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        if (appId.isBlank() || appKey.isBlank()) {
            log.warn("Adzuna credentials not configured (adzuna.app-id/adzuna.app-key blank); skipping import.");
            return List.of();
        }

        AdzunaSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/jobs/{country}/search/{page}")
                        .queryParam("app_id", appId)
                        .queryParam("app_key", appKey)
                        .queryParam("results_per_page", resultsPerPage)
                        .queryParam("content-type", "application/json")
                        .build(country, page))
                .retrieve()
                .bodyToMono(AdzunaSearchResponse.class)
                .block();

        return AdzunaMapper.toExternalJobDtos(response);
    }

    /** Mirrors the guarded method's params + a trailing Throwable, per Resilience4j's fallback contract. */
    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling Adzuna (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
