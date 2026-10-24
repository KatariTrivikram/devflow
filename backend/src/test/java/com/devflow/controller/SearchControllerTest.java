package com.devflow.controller;

import com.devflow.dto.response.SearchResponse;
import com.devflow.dto.response.SearchResult;
import com.devflow.entity.enums.SearchResourceType;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.service.SearchService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    @DisplayName("GET /api/search - returns 200 with SearchResponse on valid query")
    void testSearch_Success() throws Exception {
        SearchResult item = new SearchResult();
        item.setId(UUID.randomUUID());
        item.setType(SearchResourceType.ISSUE);
        item.setTitle("Fix auth token refresh");
        item.setDescription("Token expired race condition");
        item.setStatus("IN_PROGRESS");
        item.setPriority("HIGH");

        SearchResponse response = new SearchResponse(
                "auth", SearchResourceType.ALL, List.of(item),
                0, 10, 1L, 1, true, true
        );

        when(searchService.search(eq("auth"), eq(SearchResourceType.ALL), eq(0), eq(10), anyString()))
                .thenReturn(response);

        mockMvc.perform(get("/api/search")
                        .param("q", "auth")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("auth"))
                .andExpect(jsonPath("$.type").value("ALL"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix auth token refresh"))
                .andExpect(jsonPath("$.content[0].type").value("ISSUE"));

        verify(searchService).search(eq("auth"), eq(SearchResourceType.ALL), eq(0), eq(10), anyString());
    }

    @Test
    @DisplayName("GET /api/search - passes type, pagination, and sort parameters")
    void testSearch_WithCustomParameters() throws Exception {
        SearchResponse response = new SearchResponse(
                "devflow", SearchResourceType.PROJECT, List.of(),
                1, 5, 0L, 0, false, true
        );

        when(searchService.search(eq("devflow"), eq(SearchResourceType.PROJECT), eq(1), eq(5), eq("updatedAt,desc")))
                .thenReturn(response);

        mockMvc.perform(get("/api/search")
                        .param("q", "devflow")
                        .param("type", "PROJECT")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "updatedAt,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PROJECT"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(searchService).search(eq("devflow"), eq(SearchResourceType.PROJECT), eq(1), eq(5), eq("updatedAt,desc"));
    }

    @Test
    @DisplayName("GET /api/search - returns 400 when 'q' parameter is missing")
    void testSearch_MissingQueryParam() throws Exception {
        mockMvc.perform(get("/api/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/search - returns 400 when query is blank")
    void testSearch_BlankQuery() throws Exception {
        when(searchService.search(eq("   "), any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new BusinessRuleException("Search query must not be blank"));

        mockMvc.perform(get("/api/search")
                        .param("q", "   ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Search query must not be blank"));
    }

    @Test
    @DisplayName("GET /api/search - returns 400 on invalid resource type")
    void testSearch_InvalidType() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "auth")
                        .param("type", "INVALID_RESOURCE_TYPE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/search - returns 400 on invalid page")
    void testSearch_InvalidPage() throws Exception {
        when(searchService.search(eq("auth"), any(), eq(-1), anyInt(), anyString()))
                .thenThrow(new BusinessRuleException("Page index must not be less than zero"));

        mockMvc.perform(get("/api/search")
                        .param("q", "auth")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }
}
