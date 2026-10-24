package com.devflow.specification;

import com.devflow.entity.Issue;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueSpecificationTest {

    @Mock
    private Root<Issue> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Path<String> stringPath;

    @Mock
    private Predicate predicate;

    @Test
    @DisplayName("hasProjectId - null id returns null, non-null returns equal predicate")
    void testHasProjectId() {
        assertNull(IssueSpecification.hasProjectId(null).toPredicate(root, query, cb));

        UUID id = UUID.randomUUID();
        when(root.get("project")).thenReturn((Path) path);
        when(path.get("id")).thenReturn(path);
        when(cb.equal(path, id)).thenReturn(predicate);

        Predicate result = IssueSpecification.hasProjectId(id).toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(path, id);
    }

    @Test
    @DisplayName("hasStatus - null returns null, non-null returns equal predicate")
    void testHasStatus() {
        assertNull(IssueSpecification.hasStatus(null).toPredicate(root, query, cb));

        when(root.get("status")).thenReturn((Path) path);
        when(cb.equal(path, IssueStatus.TODO)).thenReturn(predicate);

        Predicate result = IssueSpecification.hasStatus(IssueStatus.TODO).toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(path, IssueStatus.TODO);
    }

    @Test
    @DisplayName("hasPriority - null returns null, non-null returns equal predicate")
    void testHasPriority() {
        assertNull(IssueSpecification.hasPriority(null).toPredicate(root, query, cb));

        when(root.get("priority")).thenReturn((Path) path);
        when(cb.equal(path, IssuePriority.CRITICAL)).thenReturn(predicate);

        Predicate result = IssueSpecification.hasPriority(IssuePriority.CRITICAL).toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(path, IssuePriority.CRITICAL);
    }

    @Test
    @DisplayName("hasType - null returns null, non-null returns equal predicate")
    void testHasType() {
        assertNull(IssueSpecification.hasType(null).toPredicate(root, query, cb));

        when(root.get("type")).thenReturn((Path) path);
        when(cb.equal(path, IssueType.FEATURE)).thenReturn(predicate);

        Predicate result = IssueSpecification.hasType(IssueType.FEATURE).toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(path, IssueType.FEATURE);
    }

    @Test
    @DisplayName("search - null or blank returns null, non-blank returns or predicate")
    void testSearch() {
        assertNull(IssueSpecification.search(null).toPredicate(root, query, cb));
        assertNull(IssueSpecification.search("   ").toPredicate(root, query, cb));

        when(root.get("title")).thenReturn((Path) stringPath);
        when(root.get("description")).thenReturn((Path) stringPath);
        when(cb.lower(stringPath)).thenReturn(stringPath);
        when(cb.like(stringPath, "%keyword%")).thenReturn(predicate);
        when(cb.or(predicate, predicate)).thenReturn(predicate);

        Predicate result = IssueSpecification.search("keyword").toPredicate(root, query, cb);
        assertNotNull(result);
    }

    @Test
    @DisplayName("isAccessibleByUser - ADMIN role returns conjunction")
    void testIsAccessibleByUser_Admin() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = IssueSpecification.isAccessibleByUser(UUID.randomUUID(), UserRole.ADMIN)
                .toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).conjunction();
    }
}
