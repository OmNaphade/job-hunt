package org.omnaphade.job_service.external.jobicy;

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
 * Free, no-key, read-only remote-jobs API: https://jobicy.com/api/v2/remote-jobs (docs linked from every
 * response's {@code documentationUrl} field). Jobicy doesn't paginate — {@code count} just caps how many
 * of its most-recent listings come back in one call — so {@link #fetchJobs(int)} only ever hits the
 * network for {@code page == 1}.
 */
@Component
public class JobicyClient implements ExternalJobProvider {

    private static final Logger log = LoggerFactory.getLogger(JobicyClient.class);

    private final WebClient webClient;
    private final int count;
    private final String geo;
    private final String industry;
    private final String tag;
    private final int maxPagesPerRun;

    public JobicyClient(WebClient.Builder webClientBuilder,
                         @Value("${jobicy.base-url:https://jobicy.com/api/v2}") String baseUrl,
                         @Value("${jobicy.count:50}") int count,
                         @Value("${jobicy.geo:}") String geo,
                         @Value("${jobicy.industry:}") String industry,
                         @Value("${jobicy.tag:}") String tag,
                         @Value("${jobicy.max-pages-per-run:1}") int maxPagesPerRun) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.count = count;
        this.geo = geo;
        this.industry = industry;
        this.tag = tag;
        this.maxPagesPerRun = maxPagesPerRun;
    }

    @Override
    public JobSource getSource() {
        return JobSource.JOBICY;
    }

    @Override
    public int getMaxPagesPerRun() {
        return maxPagesPerRun;
    }

    @Override
    @CircuitBreaker(name = "jobicyImport", fallbackMethod = "fetchJobsFallback")
    public List<ExternalJobDTO> fetchJobs(int page) {
        if (page > 1) {
            return List.of();
        }

        JobicySearchResponse response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/remote-jobs").queryParam("count", count);
                    if (geo != null && !geo.isBlank()) {
                        uriBuilder.queryParam("geo", geo);
                    }
                    if (industry != null && !industry.isBlank()) {
                        uriBuilder.queryParam("industry", industry);
                    }
                    if (tag != null && !tag.isBlank()) {
                        uriBuilder.queryParam("tag", tag);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(JobicySearchResponse.class)
                .block();

        return JobicyMapper.toExternalJobDtos(response);
    }

    public List<ExternalJobDTO> fetchJobsFallback(int page, Throwable ex) {
        log.warn("Circuit breaker open/error while calling jobicy (page={}): {}", page, ex.getMessage());
        return List.of();
    }

}
