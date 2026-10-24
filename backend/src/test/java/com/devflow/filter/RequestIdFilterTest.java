package com.devflow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("Request ID Filter Unit Test")
class RequestIdFilterTest {

    private RequestIdFilter requestIdFilter;

    @BeforeEach
    void setUp() {
        requestIdFilter = new RequestIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("should generate a new UUID request ID and set response header when header is absent")
    void shouldGenerateRequestIdWhenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        requestIdFilter.doFilter(request, response, filterChain);

        String responseHeader = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(responseHeader).isNotNull().isNotBlank();
        assertDoesNotThrow(() -> UUID.fromString(responseHeader));
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("should preserve existing X-Request-ID header supplied by client")
    void shouldPreserveExistingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String customRequestId = "custom-client-trace-12345";
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, customRequestId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        requestIdFilter.doFilter(request, response, filterChain);

        String responseHeader = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(responseHeader).isEqualTo(customRequestId);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("should populate MDC with requestId during request processing and clear MDC afterwards")
    void shouldPopulateAndCleanMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValueDuringFilter = new AtomicReference<>();
        FilterChain capturingChain = (req, res) -> {
            mdcValueDuringFilter.set(MDC.get(RequestIdFilter.MDC_KEY));
        };

        requestIdFilter.doFilter(request, response, capturingChain);

        String responseHeader = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(mdcValueDuringFilter.get()).isNotNull().isEqualTo(responseHeader);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
