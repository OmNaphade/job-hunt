package org.omnaphade.notification_service.dtos;

import lombok.*;
import org.omnaphade.notification_service.entities.NotificationStatus;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    private Long id;
    private Long userId;
    private String type;
    private String message;
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
