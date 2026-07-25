package org.omnaphade.notification_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.notification_service.dtos.NotificationCreateDTO;
import org.omnaphade.notification_service.dtos.NotificationResponseDTO;
import org.omnaphade.notification_service.entities.Notification;
import org.omnaphade.notification_service.entities.NotificationStatus;
import org.omnaphade.notification_service.exception.ResourceNotFoundException;
import org.omnaphade.notification_service.mapper.NotificationMapper;
import org.omnaphade.notification_service.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements INotificationService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "type", "status");

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponseDTO createNotification(NotificationCreateDTO dto) {
        Notification notification = NotificationMapper.toEntity(dto);
        notification.setStatus(NotificationStatus.UNREAD);
        return NotificationMapper.toDTO(notificationRepository.save(notification));
    }

    @Override
    public NotificationResponseDTO createApplicationStatusNotification(Long userId, String status, Long jobId) {
        String message = buildApplicationMessage(status, jobId);
        NotificationCreateDTO dto = new NotificationCreateDTO(userId, "APPLICATION_UPDATE", message);
        return createNotification(dto);
    }

    @Override
    public Page<NotificationResponseDTO> getNotificationsByUser(Long userId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return notificationRepository.findByUserId(userId, pageable).map(NotificationMapper::toDTO);
    }

    @Override
    public Page<NotificationResponseDTO> getUnreadByUser(Long userId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.UNREAD, pageable)
                .map(NotificationMapper::toDTO);
    }

    @Override
    public NotificationResponseDTO markAsRead(Long id) {
        Notification notification = findById(id);
        notification.setStatus(NotificationStatus.READ);
        return NotificationMapper.toDTO(notificationRepository.save(notification));
    }

    @Override
    public NotificationResponseDTO markAsReadForUser(Long id, Long userId, boolean admin) {
        Notification notification = admin
                ? findById(id)
                : notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found for user: " + id));

        notification.setStatus(NotificationStatus.READ);
        return NotificationMapper.toDTO(notificationRepository.save(notification));
    }

    @Override
    public long markAllAsReadForUser(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.UNREAD);
        unread.forEach(item -> item.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Override
    public void deleteNotification(Long id) {
        notificationRepository.delete(findById(id));
    }

    @Override
    public void deleteNotificationForUser(Long id, Long userId, boolean admin) {
        Notification notification = admin
                ? findById(id)
                : notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found for user: " + id));
        notificationRepository.delete(notification);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    private Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    private String buildApplicationMessage(String status, Long jobId) {
        return switch (status) {
            case "SHORTLISTED" -> "Congratulations! Your application for job #" + jobId + " has been shortlisted.";
            case "REJECTED" -> "Unfortunately, your application for job #" + jobId + " was not selected.";
            case "HIRED" -> "Great news! You've been hired for job #" + jobId + "!";
            default -> "Your application status for job #" + jobId + " has been updated to " + status;
        };
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        String normalizedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, normalizedSortBy));
    }
}
