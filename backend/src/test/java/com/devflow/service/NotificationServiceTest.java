package com.devflow.service;

import com.devflow.dto.response.NotificationResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.UnreadCountResponse;
import com.devflow.entity.Notification;
import com.devflow.entity.User;
import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.NotificationMapper;
import com.devflow.repository.NotificationRepository;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;
    private User actor;
    private UUID recipientId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        recipient = new User();
        recipient.setId(recipientId);
        recipient.setName("Recipient User");
        recipient.setEmail("recipient@devflow.io");
        recipient.setRole(UserRole.DEVELOPER);

        actor = new User();
        actor.setId(actorId);
        actor.setName("Actor User");
        actor.setEmail("actor@devflow.io");
        actor.setRole(UserRole.PROJECT_MANAGER);
    }

    @Test
    @DisplayName("createNotification - successfully persists notification when actor is not recipient")
    void testCreateNotification_Success() {
        UUID refId = UUID.randomUUID();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.createNotification(
                recipient,
                actor,
                NotificationType.ISSUE_ASSIGNED,
                "You were assigned an issue",
                "You were assigned issue \"Fix layout\"",
                NotificationReferenceType.ISSUE,
                refId
        );

        assertNotNull(result);
        assertEquals(recipient, result.getRecipient());
        assertEquals(actor, result.getActor());
        assertEquals(NotificationType.ISSUE_ASSIGNED, result.getType());
        assertEquals("You were assigned an issue", result.getTitle());
        assertEquals(NotificationReferenceType.ISSUE, result.getReferenceType());
        assertEquals(refId, result.getReferenceId());
        assertFalse(result.isRead());

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createNotification - suppresses self-action when actor equals recipient")
    void testCreateNotification_SuppressesSelfAction() {
        Notification result = notificationService.createNotification(
                actor,
                actor, // same user as recipient
                NotificationType.ISSUE_STATUS_CHANGED,
                "Status Changed",
                "Self status change",
                NotificationReferenceType.ISSUE,
                UUID.randomUUID()
        );

        assertNull(result);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("createNotifications - deduplicates recipients and filters out actor")
    void testCreateNotifications_DeduplicatesAndFiltersActor() {
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setName("User 2");

        // List contains recipient twice, actor once, and user2
        List<User> recipients = List.of(recipient, recipient, actor, user2);

        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UUID sprintId = UUID.randomUUID();
        List<Notification> result = notificationService.createNotifications(
                recipients,
                actor,
                NotificationType.SPRINT_STATUS_CHANGED,
                "Sprint status updated",
                "Sprint 1 is now ACTIVE",
                NotificationReferenceType.SPRINT,
                sprintId
        );

        // Expect exactly 2 notifications (recipient and user2; actor excluded, recipient deduplicated)
        assertEquals(2, result.size());
        verify(notificationRepository).saveAll(any());
    }

    @Test
    @DisplayName("getNotifications - retrieves paginated notifications for authenticated user")
    void testGetNotifications() {
        when(securityUtils.getCurrentUserId()).thenReturn(recipientId);

        Notification n1 = new Notification(recipient, actor, NotificationType.ISSUE_ASSIGNED, "Title", "Msg", NotificationReferenceType.ISSUE, UUID.randomUUID());
        Page<Notification> page = new PageImpl<>(List.of(n1));

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(recipientId), any(Pageable.class)))
                .thenReturn(page);

        NotificationResponse responseDto = new NotificationResponse();
        responseDto.setId(UUID.randomUUID());
        responseDto.setTitle("Title");
        when(notificationMapper.toResponse(n1)).thenReturn(responseDto);

        PageResponse<NotificationResponse> result = notificationService.getNotifications(false, 0, 20, "createdAt,desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Title", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("getUnreadCount - returns unread count for authenticated user")
    void testGetUnreadCount() {
        when(securityUtils.getCurrentUserId()).thenReturn(recipientId);
        when(notificationRepository.countByRecipientIdAndIsReadFalse(recipientId)).thenReturn(4L);

        UnreadCountResponse result = notificationService.getUnreadCount();

        assertNotNull(result);
        assertEquals(4L, result.getCount());
    }

    @Test
    @DisplayName("markAsRead - successfully marks authenticated user's notification as read")
    void testMarkAsRead_Success() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(recipient, actor, NotificationType.ISSUE_ASSIGNED, "Title", "Msg", NotificationReferenceType.ISSUE, UUID.randomUUID());
        notification.setId(notificationId);
        notification.setRead(false);

        when(securityUtils.getCurrentUserId()).thenReturn(recipientId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse responseDto = new NotificationResponse();
        responseDto.setId(notificationId);
        responseDto.setRead(true);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(responseDto);

        NotificationResponse result = notificationService.markAsRead(notificationId);

        assertNotNull(result);
        assertTrue(result.isRead());
        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead - throws AccessDeniedException when user attempts to mark another user's notification (IDOR)")
    void testMarkAsRead_IdorForbidden() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(recipient, actor, NotificationType.ISSUE_ASSIGNED, "Title", "Msg", NotificationReferenceType.ISSUE, UUID.randomUUID());
        notification.setId(notificationId);

        // Current authenticated user is someone else (actorId instead of recipientId)
        when(securityUtils.getCurrentUserId()).thenReturn(actorId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead(notificationId));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead - throws ResourceNotFoundException when notification does not exist")
    void testMarkAsRead_NotFound() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(notificationId));
    }

    @Test
    @DisplayName("markAllAsRead - marks all notifications for authenticated user in bulk")
    void testMarkAllAsRead() {
        when(securityUtils.getCurrentUserId()).thenReturn(recipientId);
        when(notificationRepository.markAllAsReadByRecipientId(eq(recipientId), any(Instant.class))).thenReturn(5);

        int updated = notificationService.markAllAsRead();

        assertEquals(5, updated);
        verify(notificationRepository).markAllAsReadByRecipientId(eq(recipientId), any(Instant.class));
    }
}
