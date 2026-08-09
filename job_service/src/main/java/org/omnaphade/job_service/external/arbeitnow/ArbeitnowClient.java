package org.omnaphade.job_service.external.arbeitnow;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.omnaphade.job_service.entities.JobSource;
import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.ExternalJobProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Free, no-key job board API: https://www.arbeitnow.com/api/job-board-api */
@Component
public class ArbeitnowClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(ArbeitnowClient.class);

    private final WebClient webClient;
    private final Set<String> techKeywords;

    public ArbeitnowClient(WebClient.Builder webClientBuilder,
                            @Value("${arbeitnow.base-url:https://www.arbeitnow.com/api/job-board-api}") String baseUrl,
                            @Value("${arbeitnow.tech-keywords:developer,engineer,software}") String techKeywordsCsv) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.techKeywords = Arrays.stream(techKeywordsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public JobSource getSource() {
        return JobSource.ARBEITNOW;
    }

    @Override
    @CircuitBreaker(name = "arbeitnowImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        ArbeitnowSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(ArbeitnowSearchResponse.class)
                .block();

        return ArbeitnowMapper.toExternalJobDtos(response, techKeywords);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling Arbeitnow (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
