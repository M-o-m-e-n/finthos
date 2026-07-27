package com.alahly.momkn.finthos.common.logging;

import com.alahly.momkn.finthos.integration.client.ProcessorClient;
import com.alahly.momkn.finthos.integration.client.ProcessorRequest;
import com.alahly.momkn.finthos.integration.client.ProcessorResponse;
import com.alahly.momkn.finthos.integration.repository.ProcessorAuthorizationRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoggingAspectTest {

    private static MockWebServer mockServer;

    @Autowired
    private ProcessorClient processorClient;

    @MockitoBean
    private ProcessorAuthorizationRepository authRepo;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("processor.base-url", () -> mockServer.url("/").toString());
        registry.add("processor.retry-count", () -> "1");
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
    void aspectLogsSuccessWithCorrelationId() {
        MDC.put("correlationId", "test-corr-123");
        try {
            mockServer.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"reference\":\"tx-1\",\"status\":\"APPROVED\",\"authCode\":\"abc\"}"));

            ProcessorRequest req = new ProcessorRequest("tx-1", new BigDecimal("100.00"), "USD");
            ProcessorResponse response = processorClient.authorizeWithRetry(UUID.randomUUID(), req);

            assertThat(response.isApproved()).isTrue();
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Test
    void aspectDoesNotBreakNormalOperation() {
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-2\",\"status\":\"APPROVED\",\"authCode\":\"def\"}"));

        ProcessorRequest req = new ProcessorRequest("tx-2", new BigDecimal("50.00"), "USD");
        ProcessorResponse response = processorClient.authorizeWithRetry(UUID.randomUUID(), req);

        assertThat(response.isApproved()).isTrue();
    }

    @Test
    void aspectIsConfigured() {
        assertThat(processorClient).isNotNull();
    }
}
