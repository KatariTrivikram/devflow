package com.devflow.specification;

import com.devflow.entity.Project;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSpecificationTest {

    @Mock
    private Root<Project> root;

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
    @DisplayName("hasStatus - null returns null, non-null returns equal predicate")
    void testHasStatus() {
        assertNull(ProjectSpecification.hasStatus(null).toPredicate(root, query, cb));

        when(root.get("status")).thenReturn((Path) path);
        when(cb.equal(path, ProjectStatus.ACTIVE)).thenReturn(predicate);

        Predicate result = ProjectSpecification.hasStatus(ProjectStatus.ACTIVE).toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(path, ProjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("search - null or blank returns null, non-blank returns like predicate")
    void testSearch() {
        assertNull(ProjectSpecification.search(null).toPredicate(root, query, cb));
        assertNull(ProjectSpecification.search("   ").toPredicate(root, query, cb));

        when(root.get("name")).thenReturn((Path) stringPath);
        when(cb.lower(stringPath)).thenReturn(stringPath);
        when(cb.like(stringPath, "%devflow%")).thenReturn(predicate);

        Predicate result = ProjectSpecification.search("devflow").toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).like(stringPath, "%devflow%");
    }

    @Test
    @DisplayName("isAccessibleByUser - ADMIN role returns conjunction")
    void testIsAccessibleByUser_Admin() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = ProjectSpecification.isAccessibleByUser(UUID.randomUUID(), UserRole.ADMIN)
                .toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).conjunction();
    }
}
