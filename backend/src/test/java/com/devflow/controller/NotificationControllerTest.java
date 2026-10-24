package com.devflow.controller;

import com.devflow.dto.response.NotificationResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.UnreadCountResponse;
import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("GET /api/notifications - returns 200 with PageResponse of notifications")
    void testGetNotifications_Success() throws Exception {
        NotificationResponse item = new NotificationResponse();
        item.setId(UUID.randomUUID());
        item.setType(NotificationType.ISSUE_ASSIGNED);
        item.setTitle("You were assigned an issue");
        item.setMessage("You were assigned issue \"Fix login\"");
        item.setReferenceType(NotificationReferenceType.ISSUE);
        item.setReferenceId(UUID.randomUUID());
        item.setRead(false);
        item.setCreatedAt(Instant.now());

        PageResponse<NotificationResponse> page = new PageResponse<>(
                List.of(item), 0, 20, 1, 1, true, true
        );

        when(notificationService.getNotifications(nullable(Boolean.class), anyInt(), anyInt(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ISSUE_ASSIGNED"))
                .andExpect(jsonPath("$.content[0].title").value("You were assigned an issue"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(notificationService).getNotifications(nullable(Boolean.class), anyInt(), anyInt(), anyString());
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count - returns 200 with unread count")
    void testGetUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(new UnreadCountResponse(5L));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));

        verify(notificationService).getUnreadCount();
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read - returns 200 with updated notification")
    void testMarkAsRead_Success() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse();
        response.setId(id);
        response.setRead(true);
        response.setReadAt(Instant.now());

        when(notificationService.markAsRead(id)).thenReturn(response);

        mockMvc.perform(patch("/api/notifications/{id}/read", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).markAsRead(id);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read - returns 403 Forbidden on IDOR attempt")
    void testMarkAsRead_IdorForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.markAsRead(id)).thenThrow(new AccessDeniedException("Access denied: You do not have permission to modify this notification"));

        mockMvc.perform(patch("/api/notifications/{id}/read", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read - returns 404 Not Found if notification does not exist")
    void testMarkAsRead_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.markAsRead(id)).thenThrow(new ResourceNotFoundException("Notification not found with id: " + id));

        mockMvc.perform(patch("/api/notifications/{id}/read", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all - returns 200 OK after bulk update")
    void testMarkAllAsRead_Success() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(3);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"))
                .andExpect(jsonPath("$.updatedCount").value(3));

        verify(notificationService).markAllAsRead();
    }
}
