package org.omnaphade.job_service.external.remotive;

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
 * Free, no-key, read-only remote-jobs API: https://remotive.com/api/remote-jobs (docs at
 * https://remotive.com/api/remote-jobs?limit=1 for the response shape). Remotive doesn't paginate the
 * response — it returns its full current listing in one call — so {@link #fetchJobs(int)} only ever hits
 * the network for {@code page == 1} and Remotive's own terms ask for at most 4 calls/day, which the
 * existing 6-hour import scheduler already matches exactly.
 */
@Component
public class RemotiveClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(RemotiveClient.class);

    private final WebClient webClient;
    private final String search;
    private final String category;
    private final int maxPagesPerRun;

    public RemotiveClient(WebClient.Builder webClientBuilder,
                           @Value("${remotive.base-url:https://remotive.com/api}") String baseUrl,
                           @Value("${remotive.search:}") String search,
                           @Value("${remotive.category:}") String category,
                           @Value("${remotive.max-pages-per-run:1}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.search = search;
        this.category = category;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.REMOTIVE;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    @Override
    @CircuitBreaker(name = "remotiveImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        if (page > 1) {
            return List.of();
        }

        RemotiveSearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/remote-jobs");
                    if (search != null && !search.isBlank()) {
                        uriBuilder.queryParam("search", search);
                    }
                    if (category != null && !category.isBlank()) {
                        uriBuilder.queryParam("category", category);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(RemotiveSearchResponse.class)
                .block();

        return RemotiveMapper.toExternalJobDtos(response);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling remotive (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
