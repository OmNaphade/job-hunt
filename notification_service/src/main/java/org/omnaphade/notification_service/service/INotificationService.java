package org.omnaphade.notification_service.service;

import org.omnaphade.notification_service.dtos.NotificationCreateDTO;
import org.omnaphade.notification_service.dtos.NotificationResponseDTO;
import org.springframework.data.domain.Page;

public interface INotificationService {
    NotificationResponseDTO createNotification(NotificationCreateDTO dto);
    NotificationResponseDTO createApplicationStatusNotification(Long userId, String status, Long jobId);
    Page<NotificationResponseDTO> getNotificationsByUser(Long userId, int page, int size, String sortBy, String sortDir);
    Page<NotificationResponseDTO> getUnreadByUser(Long userId, int page, int size, String sortBy, String sortDir);
    NotificationResponseDTO markAsRead(Long id);
    NotificationResponseDTO markAsReadForUser(Long id, Long userId, boolean admin);
    long markAllAsReadForUser(Long userId);
    void deleteNotification(Long id);
    void deleteNotificationForUser(Long id, Long userId, boolean admin);
    long getUnreadCount(Long userId);
}
