package org.omnaphade.auth_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Map;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${services.user-service.url:http://user-service/api/users}") String userServiceBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(userServiceBaseUrl).build();
    }

    // Example: create user profile
    @CircuitBreaker(name = "userServiceCall", fallbackMethod = "createUserProfileFallback")
    public void createUserProfile(Long userId, String headline, String summary) {
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "headline", headline,
                "summary", summary
        );

        webClient.post()
                .uri("/{userId}/profile", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void createUserProfileFallback(Long userId, String headline, String summary, Exception ex) {
        log.warn("Circuit breaker open - user profile creation skipped for userId={}: {}", userId, ex.getMessage());
    }

    // Example: get user profile
    @CircuitBreaker(name = "userServiceCall", fallbackMethod = "getUserProfileFallback")
    public Map<String, Object> getUserProfile(Long userId) {
        return webClient.get()
                .uri("/{userId}/profile", userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> getUserProfileFallback(Long userId, Exception ex) {
        log.warn("Circuit breaker open - get user profile skipped for userId={}: {}", userId, ex.getMessage());
        return Collections.emptyMap();
    }

    @CircuitBreaker(name = "userServiceCall", fallbackMethod = "deleteUserProfileFallback")
    public void deleteUserProfile(Long userId) {
        webClient.delete()
                .uri("/{userId}/profile", userId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void deleteUserProfileFallback(Long userId, Exception ex) {
        log.warn("Circuit breaker open - user profile deletion skipped for userId={}: {}", userId, ex.getMessage());
    }
}
