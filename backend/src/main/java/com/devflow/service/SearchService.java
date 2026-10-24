package com.devflow.service;

import com.devflow.dto.response.SearchResponse;
import com.devflow.dto.response.SearchResult;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.enums.SearchResourceType;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.security.SecurityUtils;
import com.devflow.specification.IssueSpecification;
import com.devflow.specification.ProjectSpecification;
import com.devflow.specification.SprintSpecification;
import com.devflow.util.PaginationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final int MAX_QUERY_LENGTH = 100;
    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

    public SearchService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            SprintRepository sprintRepository,
            SecurityUtils securityUtils) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.sprintRepository = sprintRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Executes global search across accessible Projects, Issues, and Sprints.
     *
     * @param query Raw search keyword
     * @param type  Resource type filter (ALL, PROJECT, ISSUE, SPRINT)
     * @param page  Zero-based page index
     * @param size  Page size
     * @param sort  Sort property and direction
     * @return Paginated SearchResponse
     */
    public SearchResponse search(String query, SearchResourceType type, int page, int size, String sort) {
        if (query == null || query.trim().isEmpty()) {
            throw new BusinessRuleException("Search query must not be blank");
        }

        String trimmedQuery = query.trim();
        if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
            throw new BusinessRuleException("Search query must not exceed " + MAX_QUERY_LENGTH + " characters");
        }

        PaginationUtils.validatePagination(page, size);

        SearchResourceType targetType = (type != null) ? type : SearchResourceType.ALL;
        Sort parsedSort = PaginationUtils.parseSort(sort, PaginationUtils.SEARCH_SORT_FIELDS, DEFAULT_SORT);

        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        log.debug("Global search: query='{}', type={}, user={}, role={}, page={}, size={}",
                trimmedQuery, targetType, currentUserId, currentUserRole, page, size);

        switch (targetType) {
            case PROJECT -> {
                return searchProjectsOnly(trimmedQuery, currentUserId, currentUserRole, page, size, parsedSort);
            }
            case ISSUE -> {
                return searchIssuesOnly(trimmedQuery, currentUserId, currentUserRole, page, size, parsedSort);
            }
            case SPRINT -> {
                return searchSprintsOnly(trimmedQuery, currentUserId, currentUserRole, page, size, parsedSort);
            }
            case ALL -> {
                return searchAll(trimmedQuery, currentUserId, currentUserRole, page, size, parsedSort);
            }
            default -> throw new BusinessRuleException("Unsupported search resource type: " + targetType);
        }
    }

    private SearchResponse searchProjectsOnly(String query, UUID userId, UserRole role, int page, int size, Sort sort) {
        Specification<Project> spec = Specification.where(ProjectSpecification.isAccessibleByUser(userId, role))
                .and(ProjectSpecification.searchGlobal(query));

        Sort projectSort = mapSortProperty(sort, "title", "name");
        Pageable pageable = PageRequest.of(page, size, projectSort);

        Page<Project> resultPage = projectRepository.findAll(spec, pageable);
        List<SearchResult> content = resultPage.getContent().stream()
                .map(this::mapProject)
                .toList();

        return new SearchResponse(query, SearchResourceType.PROJECT, content,
                resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalElements(),
                resultPage.getTotalPages(), resultPage.isFirst(), resultPage.isLast());
    }

    private SearchResponse searchIssuesOnly(String query, UUID userId, UserRole role, int page, int size, Sort sort) {
        Specification<Issue> spec = Specification.where(IssueSpecification.isAccessibleByUser(userId, role))
                .and(IssueSpecification.search(query));

        Sort issueSort = mapSortProperty(sort, "name", "title");
        Pageable pageable = PageRequest.of(page, size, issueSort);

        Page<Issue> resultPage = issueRepository.findAll(spec, pageable);
        List<SearchResult> content = resultPage.getContent().stream()
                .map(this::mapIssue)
                .toList();

        return new SearchResponse(query, SearchResourceType.ISSUE, content,
                resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalElements(),
                resultPage.getTotalPages(), resultPage.isFirst(), resultPage.isLast());
    }

    private SearchResponse searchSprintsOnly(String query, UUID userId, UserRole role, int page, int size, Sort sort) {
        Specification<Sprint> spec = Specification.where(SprintSpecification.isAccessibleByUser(userId, role))
                .and(SprintSpecification.search(query));

        Sort sprintSort = mapSortProperty(sort, "title", "name");
        Pageable pageable = PageRequest.of(page, size, sprintSort);

        Page<Sprint> resultPage = sprintRepository.findAll(spec, pageable);
        List<SearchResult> content = resultPage.getContent().stream()
                .map(this::mapSprint)
                .toList();

        return new SearchResponse(query, SearchResourceType.SPRINT, content,
                resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalElements(),
                resultPage.getTotalPages(), resultPage.isFirst(), resultPage.isLast());
    }

    private SearchResponse searchAll(String query, UUID userId, UserRole role, int page, int size, Sort sort) {
        Specification<Project> projectSpec = Specification.where(ProjectSpecification.isAccessibleByUser(userId, role))
                .and(ProjectSpecification.searchGlobal(query));
        Specification<Issue> issueSpec = Specification.where(IssueSpecification.isAccessibleByUser(userId, role))
                .and(IssueSpecification.search(query));
        Specification<Sprint> sprintSpec = Specification.where(SprintSpecification.isAccessibleByUser(userId, role))
                .and(SprintSpecification.search(query));

        long totalProjects = projectRepository.count(projectSpec);
        long totalIssues = issueRepository.count(issueSpec);
        long totalSprints = sprintRepository.count(sprintSpec);
        long totalElements = totalProjects + totalIssues + totalSprints;

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean isFirst = page == 0;
        boolean isLast = totalPages == 0 || page >= totalPages - 1;

        if (totalElements == 0 || page >= totalPages) {
            return new SearchResponse(query, SearchResourceType.ALL, List.of(), page, size, totalElements, totalPages, isFirst, isLast);
        }

        long startOffset = (long) page * size;
        long endOffset = Math.min(startOffset + size, totalElements);

        List<SearchResult> results = new ArrayList<>();

        // Segment 1: Projects [0, totalProjects)
        long seg1Start = 0;
        long seg1End = totalProjects;
        if (overlaps(startOffset, endOffset, seg1Start, seg1End)) {
            int offset = (int) Math.max(0, startOffset - seg1Start);
            int limit = (int) (Math.min(endOffset, seg1End) - Math.max(startOffset, seg1Start));
            List<Project> projects = fetchSlice(Project.class, projectSpec, offset, limit, mapSortProperty(sort, "title", "name"));
            projects.stream().map(this::mapProject).forEach(results::add);
        }

        // Segment 2: Issues [totalProjects, totalProjects + totalIssues)
        long seg2Start = totalProjects;
        long seg2End = totalProjects + totalIssues;
        if (overlaps(startOffset, endOffset, seg2Start, seg2End)) {
            int offset = (int) Math.max(0, startOffset - seg2Start);
            int limit = (int) (Math.min(endOffset, seg2End) - Math.max(startOffset, seg2Start));
            List<Issue> issues = fetchSlice(Issue.class, issueSpec, offset, limit, mapSortProperty(sort, "name", "title"));
            issues.stream().map(this::mapIssue).forEach(results::add);
        }

        // Segment 3: Sprints [totalProjects + totalIssues, totalElements)
        long seg3Start = totalProjects + totalIssues;
        long seg3End = totalElements;
        if (overlaps(startOffset, endOffset, seg3Start, seg3End)) {
            int offset = (int) Math.max(0, startOffset - seg3Start);
            int limit = (int) (Math.min(endOffset, seg3End) - Math.max(startOffset, seg3Start));
            List<Sprint> sprints = fetchSlice(Sprint.class, sprintSpec, offset, limit, mapSortProperty(sort, "title", "name"));
            sprints.stream().map(this::mapSprint).forEach(results::add);
        }

        return new SearchResponse(query, SearchResourceType.ALL, results, page, size, totalElements, totalPages, isFirst, isLast);
    }

    private boolean overlaps(long start1, long end1, long start2, long end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    private <T> List<T> fetchSlice(Class<T> clazz, Specification<T> spec, int offset, int limit, Sort sort) {
        if (entityManager == null) {
            return List.of();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(clazz);
        Root<T> root = cq.from(clazz);

        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }

        if (sort.isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order o : sort) {
                if (o.isAscending()) {
                    orders.add(cb.asc(root.get(o.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(o.getProperty())));
                }
            }
            cq.orderBy(orders);
        }

        TypedQuery<T> query = entityManager.createQuery(cq);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    private Sort mapSortProperty(Sort sort, String fromProp, String toProp) {
        if (!sort.isSorted()) {
            return sort;
        }
        List<Sort.Order> mappedOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (order.getProperty().equalsIgnoreCase(fromProp)) {
                mappedOrders.add(new Sort.Order(order.getDirection(), toProp));
            } else {
                mappedOrders.add(order);
            }
        }
        return Sort.by(mappedOrders);
    }

    private SearchResult mapProject(Project project) {
        SearchResult result = new SearchResult();
        result.setId(project.getId());
        result.setType(SearchResourceType.PROJECT);
        result.setTitle(project.getName());
        result.setDescription(project.getDescription());
        result.setProjectId(project.getId());
        result.setProjectName(project.getName());
        result.setStatus(project.getStatus() != null ? project.getStatus().name() : null);
        result.setUpdatedAt(project.getUpdatedAt());
        result.setReferenceId(project.getId());
        return result;
    }

    private SearchResult mapIssue(Issue issue) {
        SearchResult result = new SearchResult();
        result.setId(issue.getId());
        result.setType(SearchResourceType.ISSUE);
        result.setTitle(issue.getTitle());
        result.setDescription(issue.getDescription());
        if (issue.getProject() != null) {
            result.setProjectId(issue.getProject().getId());
            result.setProjectName(issue.getProject().getName());
        }
        result.setStatus(issue.getStatus() != null ? issue.getStatus().name() : null);
        result.setPriority(issue.getPriority() != null ? issue.getPriority().name() : null);
        result.setUpdatedAt(issue.getUpdatedAt());
        result.setReferenceId(issue.getId());
        return result;
    }

    private SearchResult mapSprint(Sprint sprint) {
        SearchResult result = new SearchResult();
        result.setId(sprint.getId());
        result.setType(SearchResourceType.SPRINT);
        result.setTitle(sprint.getName());
        if (sprint.getProject() != null) {
            result.setProjectId(sprint.getProject().getId());
            result.setProjectName(sprint.getProject().getName());
        }
        result.setStatus(sprint.getStatus() != null ? sprint.getStatus().name() : null);
        result.setStartDate(sprint.getStartDate());
        result.setEndDate(sprint.getEndDate());
        result.setUpdatedAt(sprint.getUpdatedAt());
        result.setReferenceId(sprint.getId());
        return result;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
