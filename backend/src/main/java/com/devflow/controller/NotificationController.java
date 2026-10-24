package com.devflow.controller;

import com.devflow.dto.response.NotificationResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.UnreadCountResponse;
import com.devflow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Endpoints for managing user notifications and unread alert counts")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get notifications for authenticated user", description = "Retrieves paginated notifications for the authenticated user, newest first by default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters"),
            @ApiResponse(responseCode = "401", description = "Full authentication required")
    })
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @Parameter(description = "Filter only unread notifications")
            @RequestParam(required = false) Boolean unreadOnly,
            @Parameter(description = "Zero-based page index (>= 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction (e.g. createdAt,desc)")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        PageResponse<NotificationResponse> response = notificationService.getNotifications(unreadOnly, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count", description = "Returns the total number of unread notifications for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Full authentication required")
    })
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UnreadCountResponse response = notificationService.getUnreadCount();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read", description = "Marks a specific notification as read. Fails with 403 Forbidden if the notification belongs to another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Full authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied: Notification belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications for the authenticated user as read in bulk.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Full authentication required")
    })
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        int updatedCount = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "updatedCount", updatedCount
        ));
    }
}
