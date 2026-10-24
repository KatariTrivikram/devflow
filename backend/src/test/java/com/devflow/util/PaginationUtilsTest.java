package com.devflow.util;

import com.devflow.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationUtilsTest {

    private static final List<String> ALLOWED_FIELDS = List.of("createdAt", "title", "status");

    @Test
    @DisplayName("validatePagination - valid bounds should succeed")
    void testValidatePagination_ValidBounds() {
        PaginationUtils.validatePagination(0, 1);
        PaginationUtils.validatePagination(0, 20);
        PaginationUtils.validatePagination(5, 100);
    }

    @Test
    @DisplayName("validatePagination - negative page should throw BusinessRuleException")
    void testValidatePagination_NegativePage() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> PaginationUtils.validatePagination(-1, 20)
        );
        assertEquals("Page index must not be less than zero", ex.getMessage());
    }

    @Test
    @DisplayName("validatePagination - zero size should throw BusinessRuleException")
    void testValidatePagination_ZeroSize() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> PaginationUtils.validatePagination(0, 0)
        );
        assertEquals("Page size must be between 1 and 100", ex.getMessage());
    }

    @Test
    @DisplayName("validatePagination - negative size should throw BusinessRuleException")
    void testValidatePagination_NegativeSize() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> PaginationUtils.validatePagination(0, -5)
        );
        assertEquals("Page size must be between 1 and 100", ex.getMessage());
    }

    @Test
    @DisplayName("validatePagination - size above maximum should throw BusinessRuleException")
    void testValidatePagination_SizeAboveMax() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> PaginationUtils.validatePagination(0, 101)
        );
        assertEquals("Page size must be between 1 and 100", ex.getMessage());
    }

    @Test
    @DisplayName("parseSort - valid asc and desc should succeed")
    void testParseSort_ValidAscAndDesc() {
        Sort sortAsc = PaginationUtils.parseSort("title,asc", ALLOWED_FIELDS, "createdAt,desc");
        assertNotNull(sortAsc);
        assertEquals(Sort.Direction.ASC, sortAsc.getOrderFor("title").getDirection());

        Sort sortDesc = PaginationUtils.parseSort("createdAt,desc", ALLOWED_FIELDS, "createdAt,desc");
        assertNotNull(sortDesc);
        assertEquals(Sort.Direction.DESC, sortDesc.getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("parseSort - missing direction defaults to asc")
    void testParseSort_DefaultDirectionAsc() {
        Sort sort = PaginationUtils.parseSort("title", ALLOWED_FIELDS, "createdAt,desc");
        assertNotNull(sort);
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("title").getDirection());
    }

    @Test
    @DisplayName("parseSort - case-insensitive direction DESC should succeed")
    void testParseSort_CaseInsensitiveDirection() {
        Sort sort = PaginationUtils.parseSort("title,DESC", ALLOWED_FIELDS, "createdAt,desc");
        assertNotNull(sort);
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("title").getDirection());
    }

    @Test
    @DisplayName("parseSort - null sortParam falls back to defaultSort")
    void testParseSort_NullSortParamUsesDefault() {
        Sort sort = PaginationUtils.parseSort(null, ALLOWED_FIELDS, "createdAt,desc");
        assertNotNull(sort);
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("parseSort - blank sortParam falls back to defaultSort")
    void testParseSort_BlankSortParamUsesDefault() {
        Sort sort = PaginationUtils.parseSort("   ", ALLOWED_FIELDS, "title,asc");
        assertNotNull(sort);
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("title").getDirection());
    }

    @Test
    @DisplayName("parseSort - null sortParam and null defaultSort yields unsorted")
    void testParseSort_NullSortAndNullDefaultYieldsUnsorted() {
        Sort sort = PaginationUtils.parseSort(null, ALLOWED_FIELDS, null);
        assertNotNull(sort);
        assertTrue(sort.isUnsorted());
    }

    @Test
    @DisplayName("parseSort - unallowed field throws BusinessRuleException")
    void testParseSort_UnallowedField() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> PaginationUtils.parseSort("password,desc", ALLOWED_FIELDS, "createdAt,desc")
        );
        assertTrue(ex.getMessage().contains("Invalid sort property: password"));
    }

    @Test
    @DisplayName("parseSort - invalid direction throws BusinessRuleException")
    void testParseSort_InvalidDirection() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> PaginationUtils.parseSort("title,invalidDirection", ALLOWED_FIELDS, "createdAt,desc")
        );
        assertTrue(ex.getMessage().contains("Invalid sort direction: invalidDirection"));
    }

    @Test
    @DisplayName("createPageable - valid inputs construct Pageable correctly")
    void testCreatePageable_Success() {
        Pageable pageable = PaginationUtils.createPageable(
                2, 25, "title,desc", ALLOWED_FIELDS, "createdAt,desc"
        );
        assertNotNull(pageable);
        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("title").getDirection());
    }
}
