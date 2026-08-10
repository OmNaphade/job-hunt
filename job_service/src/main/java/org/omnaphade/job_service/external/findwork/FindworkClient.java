package org.omnaphade.job_service.external.findwork;

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

/** Dev/tech job board, requires a free API key (Token auth): https://findwork.dev/developers/ */
@Component
public class FindworkClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(FindworkClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String search;
    private final int maxPagesPerRun;

    public FindworkClient(WebClient.Builder webClientBuilder,
                           @Value("${findwork.base-url:https://findwork.dev/api}") String baseUrl,
                           @Value("${findwork.api-key:}") String apiKey,
                           @Value("${findwork.search:}") String search,
                           @Value("${findwork.max-pages-per-run:5}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.search = search;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.FINDWORK;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    @Override
    @CircuitBreaker(name = "findworkImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        if (apiKey.isBlank()) {
            log.warn("Findwork credentials not configured (findwork.api-key blank); skipping import.");
            return List.of();
        }

        FindworkSearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/jobs/").queryParam("page", page);
                    if (!search.isBlank()) {
                        uriBuilder.queryParam("search", search);
                    }
                    return uriBuilder.build();
                })
                .header("Authorization", "Token " + apiKey)
                .retrieve()
                .bodyToMono(FindworkSearchResponse.class)
                .block();

        return FindworkMapper.toExternalJobDtos(response);
    }

    /** Mirrors the guarded method's params + a trailing Throwable, per Resilience4j's fallback contract. */
    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling Findwork (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
