package org.omnaphade.api_gateway.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringAggregationService monitoringAggregationService;

    public MonitoringController(MonitoringAggregationService monitoringAggregationService) {
        this.monitoringAggregationService = monitoringAggregationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            @RequestParam(value = "services", required = false) List<String> services,
            HttpServletRequest request
    ) {
        Object roleAttribute = request.getAttribute("role");
        String role = roleAttribute == null ? null : String.valueOf(roleAttribute);

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required for monitoring summary"));
        }

        Set<String> selected = services == null ? Set.of() : new HashSet<>(services);
        return ResponseEntity.ok(monitoringAggregationService.aggregate(selected));
    }
}
