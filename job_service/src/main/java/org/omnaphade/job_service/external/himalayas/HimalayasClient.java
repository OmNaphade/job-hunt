package org.omnaphade.job_service.external.himalayas;

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

/** Free, no-key job board explicitly focused on remote/tech roles: https://himalayas.app/api */
@Component
public class HimalayasClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(HimalayasClient.class);

    private final WebClient webClient;
    private final String query;
    private final int maxPagesPerRun;

    public HimalayasClient(WebClient.Builder webClientBuilder,
                            @Value("${himalayas.base-url:https://himalayas.app}") String baseUrl,
                            @Value("${himalayas.query:software developer}") String query,
                            @Value("${himalayas.max-pages-per-run:5}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.query = query;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.HIMALAYAS;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    @Override
    @CircuitBreaker(name = "himalayasImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        HimalayasSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/jobs/api/search")
                        .queryParam("q", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(HimalayasSearchResponse.class)
                .block();

        return HimalayasMapper.toExternalJobDtos(response);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling Himalayas (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
