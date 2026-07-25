package org.omnaphade.notification_service.mapper;

import org.omnaphade.notification_service.dtos.NotificationCreateDTO;
import org.omnaphade.notification_service.dtos.NotificationResponseDTO;
import org.omnaphade.notification_service.entities.Notification;

public class NotificationMapper {

    public static NotificationResponseDTO toDTO(Notification notification) {

        NotificationResponseDTO dto = new NotificationResponseDTO();

        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setStatus(notification.getStatus());
        dto.setCreatedAt(notification.getCreatedAt());

        return dto;
    }

    public static Notification toEntity(NotificationCreateDTO dto) {

        Notification notification = new Notification();

        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setMessage(dto.getMessage());

        return notification;
    }

}