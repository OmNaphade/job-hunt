package org.omnaphade.notification_service.kafka;

import lombok.RequiredArgsConstructor;
import org.omnaphade.notification_service.service.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final INotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "application-events", groupId = "notification-group")
    public void consumeApplicationEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long userId = node.get("userId").asLong();
            String status = node.get("status").asText();
            Long jobId = node.get("jobId").asLong();

            notificationService.createApplicationStatusNotification(userId, status, jobId);
            log.info("Notification created for user {} - status change to {}", userId, status);
        } catch (Exception e) {
            log.error("Failed to process application event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "job-events", groupId = "notification-group")
    public void consumeJobEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "JOB_UPDATE";
            log.info("Received job event: {}", eventType);
            // Future: notify matching job seekers
        } catch (Exception e) {
            log.error("Failed to process job event: {}", e.getMessage(), e);
        }
    }
}
