package org.omnaphade.job_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Job boards routinely return full, un-truncated HTML job descriptions (Findwork's default page size
     * alone runs ~650KB), well past Spring's 256KB default in-memory buffer limit — that limit is raised
     * here, once, for every external job provider built from this shared builder, rather than duplicated
     * per-client.
     */
    private static final int MAX_IN_MEMORY_SIZE_BYTES = 4 * 1024 * 1024;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES));
    }

}
