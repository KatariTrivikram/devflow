package com.devflow.service;

import com.devflow.dto.response.SearchResponse;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SearchResourceType;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SearchService searchService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("search - returns projects when type is PROJECT")
    void search_ProjectOnly_Success() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("DevFlow Core");
        project.setDescription("Core repository");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setUpdatedAt(Instant.now());

        when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));

        SearchResponse response = searchService.search("core", SearchResourceType.PROJECT, 0, 10, "updatedAt,desc");

        assertNotNull(response);
        assertEquals("core", response.getQuery());
        assertEquals(SearchResourceType.PROJECT, response.getType());
        assertEquals(1, response.getContent().size());
        assertEquals(project.getId(), response.getContent().get(0).getId());
        assertEquals("DevFlow Core", response.getContent().get(0).getTitle());
        assertEquals(SearchResourceType.PROJECT, response.getContent().get(0).getType());
        assertEquals("ACTIVE", response.getContent().get(0).getStatus());

        verify(projectRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("search - returns issues when type is ISSUE")
    void search_IssueOnly_Success() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Auth Service");

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setTitle("Fix JWT expiration");
        issue.setDescription("Token expires prematurely");
        issue.setProject(project);
        issue.setStatus(IssueStatus.IN_PROGRESS);
        issue.setPriority(IssuePriority.HIGH);
        issue.setUpdatedAt(Instant.now());

        when(issueRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(issue)));

        SearchResponse response = searchService.search("JWT", SearchResourceType.ISSUE, 0, 10, "updatedAt,desc");

        assertNotNull(response);
        assertEquals("JWT", response.getQuery());
        assertEquals(SearchResourceType.ISSUE, response.getType());
        assertEquals(1, response.getContent().size());
        assertEquals(issue.getId(), response.getContent().get(0).getId());
        assertEquals("Fix JWT expiration", response.getContent().get(0).getTitle());
        assertEquals("Auth Service", response.getContent().get(0).getProjectName());
        assertEquals("IN_PROGRESS", response.getContent().get(0).getStatus());
        assertEquals("HIGH", response.getContent().get(0).getPriority());

        verify(issueRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("search - returns sprints when type is SPRINT")
    void search_SprintOnly_Success() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Payments");

        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID());
        sprint.setName("Sprint 10 - Launch");
        sprint.setProject(project);
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartDate(Instant.now());
        sprint.setEndDate(Instant.now().plusSeconds(86400 * 14));
        sprint.setUpdatedAt(Instant.now());

        when(sprintRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sprint)));

        SearchResponse response = searchService.search("Launch", SearchResourceType.SPRINT, 0, 10, "updatedAt,desc");

        assertNotNull(response);
        assertEquals("Launch", response.getQuery());
        assertEquals(SearchResourceType.SPRINT, response.getType());
        assertEquals(1, response.getContent().size());
        assertEquals(sprint.getId(), response.getContent().get(0).getId());
        assertEquals("Sprint 10 - Launch", response.getContent().get(0).getTitle());
        assertEquals("ACTIVE", response.getContent().get(0).getStatus());

        verify(sprintRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("search - returns empty SearchResponse when type is ALL and counts are zero")
    void search_TypeAll_ZeroResults() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);

        when(projectRepository.count(any(Specification.class))).thenReturn(0L);
        when(issueRepository.count(any(Specification.class))).thenReturn(0L);
        when(sprintRepository.count(any(Specification.class))).thenReturn(0L);

        SearchResponse response = searchService.search("nonexistent", SearchResourceType.ALL, 0, 10, "updatedAt,desc");

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.getContent().isEmpty());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("search - throws BusinessRuleException when query is null or blank")
    void search_BlankQuery_ThrowsException() {
        assertThrows(BusinessRuleException.class, () ->
                searchService.search(null, SearchResourceType.ALL, 0, 10, "updatedAt,desc"));

        assertThrows(BusinessRuleException.class, () ->
                searchService.search("", SearchResourceType.ALL, 0, 10, "updatedAt,desc"));

        assertThrows(BusinessRuleException.class, () ->
                searchService.search("   ", SearchResourceType.ALL, 0, 10, "updatedAt,desc"));
    }

    @Test
    @DisplayName("search - throws BusinessRuleException when query exceeds maximum length")
    void search_TooLongQuery_ThrowsException() {
        String longQuery = "a".repeat(101);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                searchService.search(longQuery, SearchResourceType.ALL, 0, 10, "updatedAt,desc"));

        assertTrue(ex.getMessage().contains("exceed 100 characters"));
    }

    @Test
    @DisplayName("search - throws BusinessRuleException on invalid pagination")
    void search_InvalidPagination_ThrowsException() {
        assertThrows(BusinessRuleException.class, () ->
                searchService.search("valid", SearchResourceType.ALL, -1, 10, "updatedAt,desc"));

        assertThrows(BusinessRuleException.class, () ->
                searchService.search("valid", SearchResourceType.ALL, 0, 0, "updatedAt,desc"));

        assertThrows(BusinessRuleException.class, () ->
                searchService.search("valid", SearchResourceType.ALL, 0, 101, "updatedAt,desc"));
    }

    @Test
    @DisplayName("search - throws BusinessRuleException on invalid sort property")
    void search_InvalidSort_ThrowsException() {
        assertThrows(BusinessRuleException.class, () ->
                searchService.search("valid", SearchResourceType.PROJECT, 0, 10, "password,desc"));
    }
}
