package org.omnaphade.job_service.external.freehire;

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

/** Free, no-key, read-only job search API over freehire's IT job catalogue: https://freehire.me/docs/api */
@Component
public class FreehireClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(FreehireClient.class);

    private final WebClient webClient;
    private final int limit;
    private final String query;
    private final String workMode;
    private final int maxPagesPerRun;

    public FreehireClient(WebClient.Builder webClientBuilder,
                           @Value("${freehire.base-url:https://freehire.me/api/v1}") String baseUrl,
                           @Value("${freehire.limit:50}") int limit,
                           @Value("${freehire.query:}") String query,
                           @Value("${freehire.work-mode:}") String workMode,
                           @Value("${freehire.max-pages-per-run:5}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.limit = limit;
        this.query = query;
        this.workMode = workMode;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.FREEHIRE;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    /** {@code page} is translated to an {@code offset} since this API paginates by offset/limit. */
    @Override
    @CircuitBreaker(name = "freehireImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        int offset = Math.max(0, page - 1) * limit;

        FreehireSearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/agent/jobs/search")
                            .queryParam("limit", limit)
                            .queryParam("offset", offset);
                    if (query != null && !query.isBlank()) {
                        uriBuilder.queryParam("q", query);
                    }
                    if (workMode != null && !workMode.isBlank()) {
                        uriBuilder.queryParam("work_mode", workMode);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(FreehireSearchResponse.class)
                .block();

        return FreehireMapper.toExternalJobDtos(response);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling freehire (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
