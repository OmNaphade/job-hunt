package org.omnaphade.api_gateway.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MonitoringAggregationService {

    private final RestClient restClient;
    private final Map<String, ServiceTarget> targets;

    public MonitoringAggregationService(
            @Value("${monitoring.targets.gateway:http://localhost:8080}") String gatewayBase,
            @Value("${monitoring.targets.auth:http://localhost:8081}") String authBase,
            @Value("${monitoring.targets.user:http://localhost:8082}") String userBase,
            @Value("${monitoring.targets.job:http://localhost:8083}") String jobBase,
            @Value("${monitoring.targets.company:http://localhost:8084}") String companyBase,
            @Value("${monitoring.targets.application:http://localhost:8085}") String applicationBase,
            @Value("${monitoring.targets.notification:http://localhost:8086}") String notificationBase
    ) {
        this.restClient = RestClient.builder().build();
        this.targets = new LinkedHashMap<>();
        targets.put("gateway", new ServiceTarget("gateway", "API Gateway", gatewayBase));
        targets.put("auth", new ServiceTarget("auth", "Auth Service", authBase));
        targets.put("user", new ServiceTarget("user", "User Service", userBase));
        targets.put("job", new ServiceTarget("job", "Job Service", jobBase));
        targets.put("company", new ServiceTarget("company", "Company Service", companyBase));
        targets.put("application", new ServiceTarget("application", "Application Service", applicationBase));
        targets.put("notification", new ServiceTarget("notification", "Notification Service", notificationBase));
    }

    public MonitoringSummary aggregate(Set<String> requestedServices) {
        List<ServiceSnapshot> services = new ArrayList<>();
        Instant sampledAt = Instant.now();

        for (ServiceTarget target : targets.values()) {
            if (!requestedServices.isEmpty() && !requestedServices.contains(target.key())) {
                continue;
            }

            services.add(fetchSnapshot(target, sampledAt));
        }

        return new MonitoringSummary(sampledAt, services);
    }

    private ServiceSnapshot fetchSnapshot(ServiceTarget target, Instant sampledAt) {
        try {
            Map<?, ?> health = restClient.get()
                    .uri(target.baseUrl() + "/actuator/health")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            String prometheus = restClient.get()
                    .uri(target.baseUrl() + "/actuator/prometheus")
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);

            String status = health != null && health.get("status") != null
                    ? String.valueOf(health.get("status"))
                    : "UNKNOWN";

            String text = prometheus == null ? "" : prometheus;
            double heapBytes = parseMetric(text, "jvm_memory_used_bytes", "area=\"heap\"");
            double cpuRaw = parseMetric(text, "process_cpu_usage", null);
            double dbConnections = parseMetric(text, "hikaricp_connections_active", null);
            double requestCount = parseMetric(text, "http_server_requests_seconds_count", null);
            double circuitOpen = parseMetric(text, "resilience4j_circuitbreaker_state", "state=\"open\"");
            double threads = parseMetric(text, "jvm_threads_live_threads", null);
            double gcPauses = parseMetric(text, "jvm_gc_pause_seconds_count", null);

            return new ServiceSnapshot(
                    target.key(),
                    target.label(),
                    status,
                    roundOneDecimal(heapBytes / (1024d * 1024d)),
                    roundTwoDecimal(cpuRaw * 100d),
                    dbConnections,
                    requestCount,
                    circuitOpen,
                    threads,
                    gcPauses,
                    sampledAt.toString(),
                    null
            );
        } catch (Exception ex) {
            return new ServiceSnapshot(
                    target.key(),
                    target.label(),
                    "DOWN",
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    sampledAt.toString(),
                    ex.getMessage()
            );
        }
    }

    private double parseMetric(String prometheusText, String metricName, String mustContainLabel) {
        double total = 0d;
        String[] lines = prometheusText.split("\\n");

        for (String line : lines) {
            if (!line.startsWith(metricName)) {
                continue;
            }

            if (mustContainLabel != null && !line.contains(mustContainLabel)) {
                continue;
            }

            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace < 0 || lastSpace >= line.length() - 1) {
                continue;
            }

            String valueText = line.substring(lastSpace + 1).trim();
            try {
                total += Double.parseDouble(valueText);
            } catch (NumberFormatException ignored) {
                // Ignore malformed metric lines.
            }
        }

        return total;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private double roundTwoDecimal(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private record ServiceTarget(String key, String label, String baseUrl) {
    }

    public record MonitoringSummary(Instant sampledAt, List<ServiceSnapshot> services) {
    }

    public record ServiceSnapshot(
            String key,
            String label,
            String status,
            double heapMb,
            double cpuPercent,
            double dbConnections,
            double requestCount,
            double circuitOpen,
            double threads,
            double gcPauses,
            String sampledAt,
            String error
    ) {
    }
}
