package com.alahly.momkn.finthos.integration.client;

import com.alahly.momkn.finthos.common.error.ProcessorTimeoutException;
import com.alahly.momkn.finthos.common.error.ProcessorUnavailableException;
import com.alahly.momkn.finthos.integration.repository.ProcessorAuthorizationRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProcessorClientCircuitBreakerTest {

    private static MockWebServer mockServer;

    @Autowired
    private ProcessorClient processorClient;

    @MockitoBean
    private ProcessorAuthorizationRepository authRepo;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("processor.base-url", () -> mockServer.url("/").toString());
        registry.add("processor.retry-count", () -> "3");
        registry.add("processor.retry-delay-ms", () -> "50");
    }

    @BeforeAll
    static void startServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void circuitBreakerOpensAfterRepeatedFailures() {
        // 3 calls x 3 retry attempts each
        for (int i = 0; i < 9; i++) {
            mockServer.enqueue(new MockResponse().setResponseCode(500));
        }

        ProcessorRequest req = new ProcessorRequest("tx-4", new BigDecimal("100.00"), "USD");
        UUID txId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> processorClient.authorizeWithCircuitBreaker(txId, req))
                    .isInstanceOf(ProcessorTimeoutException.class);
        }

        assertThatThrownBy(() -> processorClient.authorizeWithCircuitBreaker(txId, req))
                .isInstanceOf(ProcessorUnavailableException.class);
    }

    @Test
    void circuitBreakerClosedOnSuccessfulCall() {
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-5\",\"status\":\"APPROVED\",\"authCode\":\"ok\"}"));

        ProcessorRequest req = new ProcessorRequest("tx-5", new BigDecimal("100.00"), "USD");
        processorClient.authorizeWithCircuitBreaker(UUID.randomUUID(), req);
    }
}
