package com.alahly.momkn.finthos.integration.client;

import com.alahly.momkn.finthos.common.error.ProcessorDeclinedException;
import com.alahly.momkn.finthos.common.error.ProcessorTimeoutException;
import com.alahly.momkn.finthos.integration.repository.ProcessorAuthorizationRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProcessorClientRetryTest {

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
    void retrySucceedsOnSecondAttempt() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-1\",\"status\":\"APPROVED\",\"authCode\":\"abc\"}"));

        ProcessorRequest req = new ProcessorRequest("tx-1", new BigDecimal("100.00"), "USD");
        ProcessorResponse response = processorClient.authorizeWithRetry(UUID.randomUUID(), req);

        assertThat(response.isApproved()).isTrue();
        assertThat(response.authCode()).isEqualTo("abc");
    }

    @Test
    void retryExhaustedThrowsTimeout() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        ProcessorRequest req = new ProcessorRequest("tx-2", new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> processorClient.authorizeWithRetry(UUID.randomUUID(), req))
                .isInstanceOf(ProcessorTimeoutException.class);
    }

    @Test
    void declinedNotRetried() {
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-3\",\"status\":\"DECLINED\",\"authCode\":null}"));

        ProcessorRequest req = new ProcessorRequest("tx-3", new BigDecimal("50.00"), "USD");

        assertThatThrownBy(() -> processorClient.authorizeWithRetry(UUID.randomUUID(), req))
                .isInstanceOf(ProcessorDeclinedException.class);
    }
}
