package com.stockpilot.infrastructure.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String runAndCaptureMdc(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);
        return seen.get();
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String inChain = runAndCaptureMdc(new MockHttpServletRequest(), response);

        assertThat(inChain).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(inChain);
    }

    @Test
    void shouldPropagateIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "client-id-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String inChain = runAndCaptureMdc(request, response);

        assertThat(inChain).isEqualTo("client-id-42");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-id-42");
    }

    @Test
    void shouldReplaceUnsafeIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "bad value\nwith newline");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String inChain = runAndCaptureMdc(request, response);

        assertThat(inChain).doesNotContain("\n").doesNotContain(" ");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(inChain);
    }

    @Test
    void shouldClearMdcAfterTheRequest() throws Exception {
        runAndCaptureMdc(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldClearMdcEvenWhenTheChainFails() {
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), failing);
        } catch (Exception expected) {
            // expected
        }

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
