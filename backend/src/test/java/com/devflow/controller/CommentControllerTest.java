package com.devflow.controller;

import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.CommentUpdateRequest;
import com.devflow.dto.response.CommentResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.service.CommentService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("POST /api/comments - successful create returns 201")
    void testCreateComment_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CommentCreateRequest request = new CommentCreateRequest();
        request.setIssueId(issueId);
        request.setUserId(userId);
        request.setContent("LGTM");

        CommentResponse response = new CommentResponse();
        UUID commentId = UUID.randomUUID();
        response.setId(commentId);
        response.setContent("LGTM");
        response.setIssueId(issueId);

        when(commentService.createComment(any(CommentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value("LGTM"));
    }

    @Test
    @DisplayName("POST /api/comments - validation failure returns 400")
    void testCreateComment_ValidationFailure() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        // missing issueId, userId, content

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("GET /api/comments/{id} - returns 200")
    void testGetCommentById_Success() throws Exception {
        UUID commentId = UUID.randomUUID();
        CommentResponse response = new CommentResponse();
        response.setId(commentId);
        response.setContent("Looks good");

        when(commentService.getCommentById(commentId)).thenReturn(response);

        mockMvc.perform(get("/api/comments/{id}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId.toString()));
    }

    @Test
    @DisplayName("GET /api/comments/issue/{issueId} - returns 200 page")
    void testGetCommentsByIssue_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        Page<CommentResponse> page = new PageImpl<>(List.of(new CommentResponse()));

        when(commentService.getCommentsByIssue(eq(issueId), anyInt(), anyInt(), any()))
                .thenReturn(PageResponse.of(page));

        mockMvc.perform(get("/api/comments/issue/{issueId}", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/comments/{id} - returns 200")
    void testUpdateComment_Success() throws Exception {
        UUID commentId = UUID.randomUUID();
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setContent("Updated content");

        CommentResponse response = new CommentResponse();
        response.setId(commentId);
        response.setContent("Updated content");

        when(commentService.updateComment(eq(commentId), any(CommentUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/comments/{id}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"));
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - returns 204")
    void testDeleteComment_Success() throws Exception {
        UUID commentId = UUID.randomUUID();
        doNothing().when(commentService).deleteComment(commentId);

        mockMvc.perform(delete("/api/comments/{id}", commentId))
                .andExpect(status().isNoContent());
    }
}
