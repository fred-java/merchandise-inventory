package com.mateus.merchandiseinventory.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RequestTracingFilterTest {

    private final RequestTracingFilter filter = new RequestTracingFilter();

    @Test
    void generatesARequestIdAndExposesItDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenByTheChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> idSeenByTheChain.set(MDC.get(RequestTracingFilter.REQUEST_ID_KEY)));

        String requestId = response.getHeader(RequestTracingFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank().isEqualTo(idSeenByTheChain.get());
        assertThat(MDC.get(RequestTracingFilter.REQUEST_ID_KEY)).isNull();
    }

    @Test
    void keepsTheRequestIdSentByTheClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(RequestTracingFilter.REQUEST_ID_HEADER, "pedido-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestTracingFilter.REQUEST_ID_HEADER)).isEqualTo("pedido-42");
    }

    @Test
    void replacesARequestIdWithUnexpectedCharacters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(RequestTracingFilter.REQUEST_ID_HEADER, "id\nfalso INFO usuario=admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestTracingFilter.REQUEST_ID_HEADER))
                .doesNotContain("\n")
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void logsMethodPathStatusAndDuration(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.setQueryString("page=2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(404));

        assertThat(output).containsPattern("GET /api/v1/products\\?page=2 respondeu 404 em \\d+ ms");
    }

    @Test
    void ignoresRequestsOutsideTheApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestTracingFilter.REQUEST_ID_HEADER)).isNull();
    }
}
