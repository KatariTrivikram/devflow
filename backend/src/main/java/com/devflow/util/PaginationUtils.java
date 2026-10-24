package com.devflow.util;

import com.devflow.exception.BusinessRuleException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static final List<String> ISSUE_SORT_FIELDS = List.of(
            "createdAt", "updatedAt", "title", "priority", "status", "type"
    );

    public static final List<String> PROJECT_SORT_FIELDS = List.of(
            "createdAt", "updatedAt", "name", "status"
    );

    public static final List<String> SPRINT_SORT_FIELDS = List.of(
            "startDate", "endDate", "name", "status", "createdAt", "updatedAt"
    );

    public static final List<String> COMMENT_SORT_FIELDS = List.of(
            "createdAt", "updatedAt"
    );

    public static final List<String> ACTIVITY_SORT_FIELDS = List.of(
            "createdAt", "action"
    );

    public static final List<String> NOTIFICATION_SORT_FIELDS = List.of(
            "createdAt", "title", "type", "isRead"
    );

    public static final List<String> SEARCH_SORT_FIELDS = List.of(
            "updatedAt", "createdAt", "title", "name"
    );

    private PaginationUtils() {
    }

    public static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BusinessRuleException("Page index must not be less than zero");
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new BusinessRuleException("Page size must be between 1 and " + MAX_SIZE);
        }
    }

    public static Sort parseSort(String sortParam, List<String> allowedFields, String defaultSort) {
        String effectiveSort = (sortParam != null && !sortParam.isBlank()) ? sortParam : defaultSort;
        if (effectiveSort == null || effectiveSort.isBlank()) {
            return Sort.unsorted();
        }

        String[] parts = effectiveSort.split(",");
        String field = parts[0].trim();
        String rawDirection = parts.length > 1 ? parts[1].trim() : "asc";

        if (!allowedFields.contains(field)) {
            throw new BusinessRuleException("Invalid sort property: " + field + ". Allowed fields: " + allowedFields);
        }

        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(rawDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(rawDirection)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new BusinessRuleException("Invalid sort direction: " + rawDirection + ". Must be 'asc' or 'desc'");
        }

        return Sort.by(direction, field);
    }

    public static Pageable createPageable(int page, int size, String sortParam, List<String> allowedFields, String defaultSort) {
        validatePagination(page, size);
        Sort sort = parseSort(sortParam, allowedFields, defaultSort);
        return PageRequest.of(page, size, sort);
    }
}
