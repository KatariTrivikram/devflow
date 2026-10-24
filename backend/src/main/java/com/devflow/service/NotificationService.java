package com.devflow.service;

import com.devflow.dto.response.NotificationResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.UnreadCountResponse;
import com.devflow.entity.Notification;
import com.devflow.entity.User;
import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.NotificationMapper;
import com.devflow.repository.NotificationRepository;
import com.devflow.security.SecurityUtils;
import com.devflow.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SecurityUtils securityUtils;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationMapper notificationMapper,
                               SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.securityUtils = securityUtils;
    }

    /**
     * Create and persist a single notification for a recipient.
     * Never notifies the actor about their own action.
     */
    @Transactional
    public Notification createNotification(User recipient,
                                           User actor,
                                           NotificationType type,
                                           String title,
                                           String message,
                                           NotificationReferenceType referenceType,
                                           UUID referenceId) {
        if (recipient == null) {
            return null;
        }

        // Never notify actor about their own action
        if (actor != null && recipient.getId().equals(actor.getId())) {
            return null;
        }

        Notification notification = new Notification(
                recipient,
                actor,
                type,
                title,
                message,
                referenceType,
                referenceId
        );

        Notification saved = notificationRepository.save(notification);
        log.debug("Created notification: id={}, type={}, recipientId={}", saved.getId(), type, recipient.getId());
        return saved;
    }

    /**
     * Create notifications for multiple recipients with automatic recipient deduplication.
     * Excludes the acting user.
     */
    @Transactional
    public List<Notification> createNotifications(Collection<User> recipients,
                                                  User actor,
                                                  NotificationType type,
                                                  String title,
                                                  String message,
                                                  NotificationReferenceType referenceType,
                                                  UUID referenceId) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }

        UUID actorId = actor != null ? actor.getId() : null;
        Map<UUID, User> deduplicated = new HashMap<>();

        for (User recipient : recipients) {
            if (recipient != null && recipient.getId() != null) {
                // Skip if recipient is the acting user
                if (actorId != null && actorId.equals(recipient.getId())) {
                    continue;
                }
                deduplicated.putIfAbsent(recipient.getId(), recipient);
            }
        }

        List<Notification> notificationsToSave = new ArrayList<>();
        for (User recipient : deduplicated.values()) {
            notificationsToSave.add(new Notification(
                    recipient,
                    actor,
                    type,
                    title,
                    message,
                    referenceType,
                    referenceId
            ));
        }

        if (notificationsToSave.isEmpty()) {
            return List.of();
        }

        List<Notification> saved = notificationRepository.saveAll(notificationsToSave);
        log.debug("Created {} notifications for type={}", saved.size(), type);
        return saved;
    }

    /**
     * Retrieve paginated notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Boolean unreadOnly, int page, int size, String sort) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.NOTIFICATION_SORT_FIELDS, "createdAt,desc"
        );

        Page<Notification> notificationPage;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notificationPage = notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(
                    currentUserId, false, pageable
            );
        } else {
            notificationPage = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                    currentUserId, pageable
            );
        }

        return PageResponse.of(notificationPage.map(notificationMapper::toResponse));
    }

    /**
     * Get the count of unread notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(currentUserId);
        return new UnreadCountResponse(count);
    }

    /**
     * Mark a single notification as read.
     * IDOR protection: Verifies that the notification belongs to the authenticated user.
     */
    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied: You do not have permission to modify this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return notificationMapper.toResponse(notification);
    }

    /**
     * Mark all notifications for the authenticated user as read in a single query.
     */
    @Transactional
    public int markAllAsRead() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return notificationRepository.markAllAsReadByRecipientId(currentUserId, Instant.now());
    }
}
