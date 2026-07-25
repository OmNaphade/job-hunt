package org.omnaphade.application_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.application-events:application-events}")
    private String topic;

    public void publishStatusChanged(Long userId, Long jobId, String status, Long applicationId) {
        String payload = String.format(
            "{\"userId\":%d,\"jobId\":%d,\"status\":\"%s\",\"applicationId\":%d}",
            userId, jobId, status, applicationId
        );
        try {
            kafkaTemplate.send(topic, payload);
            log.info("Published application event: {}", payload);
        } catch (Exception e) {
            log.warn("Failed to publish application event (Kafka may be down): {}", e.getMessage());
        }
    }
}
