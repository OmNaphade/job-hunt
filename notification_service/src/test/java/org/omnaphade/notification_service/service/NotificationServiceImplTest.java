package org.omnaphade.notification_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omnaphade.notification_service.dtos.NotificationCreateDTO;
import org.omnaphade.notification_service.dtos.NotificationResponseDTO;
import org.omnaphade.notification_service.entities.Notification;
import org.omnaphade.notification_service.entities.NotificationStatus;
import org.omnaphade.notification_service.exception.ResourceNotFoundException;
import org.omnaphade.notification_service.repository.NotificationRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    private Notification savedNotification() {
        return Notification.builder()
                .id(1L)
                .userId(9L)
                .type("APPLICATION_UPDATE")
                .message("Your application status for job #7 has been updated to REVIEWED")
                .status(NotificationStatus.UNREAD)
                .build();
    }

    @Test
    void createNotification_forcesStatusUnread() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification());

        NotificationCreateDTO dto = new NotificationCreateDTO(9L, "APPLICATION_UPDATE", "Some message");
        notificationService.createNotification(dto);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    void createApplicationStatusNotification_shortlisted_buildsCongratulatoryMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.createApplicationStatusNotification(9L, "SHORTLISTED", 7L);

        assertThat(result.getMessage()).contains("shortlisted").contains("#7");
        assertThat(result.getType()).isEqualTo("APPLICATION_UPDATE");
    }

    @Test
    void createApplicationStatusNotification_rejected_buildsRejectionMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.createApplicationStatusNotification(9L, "REJECTED", 7L);

        assertThat(result.getMessage()).contains("not selected");
    }

    @Test
    void createApplicationStatusNotification_hired_buildsHiredMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.createApplicationStatusNotification(9L, "HIRED", 7L);

        assertThat(result.getMessage()).contains("hired");
    }

    @Test
    void createApplicationStatusNotification_unknownStatus_buildsGenericMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.createApplicationStatusNotification(9L, "REVIEWED", 7L);

        assertThat(result.getMessage()).contains("updated to REVIEWED");
    }

    @Test
    void getNotificationsByUser_appliesDefaultSortWhenFieldUnknown() {
        when(notificationRepository.findByUserId(eq(9L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedNotification())));

        notificationService.getNotificationsByUser(9L, 0, 20, "bogusField", "asc");

        verify(notificationRepository).findByUserId(eq(9L), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getNotificationsByUser_clampsNegativePageAndSize() {
        when(notificationRepository.findByUserId(eq(9L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        notificationService.getNotificationsByUser(9L, -3, -1, "createdAt", "desc");

        verify(notificationRepository).findByUserId(eq(9L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void getUnreadByUser_delegatesToUnreadStatusQuery() {
        Page<Notification> page = new PageImpl<>(List.of(savedNotification()));
        when(notificationRepository.findByUserIdAndStatus(eq(9L), eq(NotificationStatus.UNREAD), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponseDTO> result = notificationService.getUnreadByUser(9L, 0, 20, "createdAt", "desc");

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void markAsRead_whenFound_updatesStatusToRead() {
        Notification existing = savedNotification();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.markAsRead(1L);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    void markAsRead_whenNotFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsReadForUser_whenOwner_updatesStatus() {
        Notification existing = savedNotification();
        when(notificationRepository.findByIdAndUserId(1L, 9L)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.markAsReadForUser(1L, 9L, false);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    void markAsReadForUser_whenNotOwnerAndNotAdmin_throwsResourceNotFoundException() {
        when(notificationRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsReadForUser(1L, 42L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsReadForUser_whenAdmin_bypassesOwnershipCheck() {
        Notification existing = savedNotification();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.markAsReadForUser(1L, 42L, true);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(notificationRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void markAllAsReadForUser_marksEveryUnreadNotificationAsRead() {
        Notification n1 = savedNotification();
        Notification n2 = Notification.builder().id(2L).userId(9L).status(NotificationStatus.UNREAD).build();
        when(notificationRepository.findByUserIdAndStatus(9L, NotificationStatus.UNREAD))
                .thenReturn(List.of(n1, n2));

        long count = notificationService.markAllAsReadForUser(9L);

        assertThat(count).isEqualTo(2);
        assertThat(n1.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(n2.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void deleteNotification_whenFound_deletesIt() {
        Notification existing = savedNotification();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(existing));

        notificationService.deleteNotification(1L);

        verify(notificationRepository).delete(existing);
    }

    @Test
    void deleteNotification_whenNotFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteNotificationForUser_whenOwner_deletesIt() {
        Notification existing = savedNotification();
        when(notificationRepository.findByIdAndUserId(1L, 9L)).thenReturn(Optional.of(existing));

        notificationService.deleteNotificationForUser(1L, 9L, false);

        verify(notificationRepository).delete(existing);
    }

    @Test
    void deleteNotificationForUser_whenNotOwnerAndNotAdmin_throwsResourceNotFoundException() {
        when(notificationRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotificationForUser(1L, 42L, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void deleteNotificationForUser_whenAdmin_bypassesOwnershipCheck() {
        Notification existing = savedNotification();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(existing));

        notificationService.deleteNotificationForUser(1L, 42L, true);

        verify(notificationRepository).delete(existing);
        verify(notificationRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void getUnreadCount_delegatesToRepositoryCount() {
        when(notificationRepository.countByUserIdAndStatus(9L, NotificationStatus.UNREAD)).thenReturn(4L);

        long result = notificationService.getUnreadCount(9L);

        assertThat(result).isEqualTo(4L);
    }
}
