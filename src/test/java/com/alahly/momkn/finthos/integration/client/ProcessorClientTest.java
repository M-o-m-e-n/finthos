package com.alahly.momkn.finthos.integration.client;

import com.alahly.momkn.finthos.common.error.ProcessorDeclinedException;
import com.alahly.momkn.finthos.common.error.ProcessorTimeoutException;
import com.alahly.momkn.finthos.integration.config.ProcessorProperties;
import com.alahly.momkn.finthos.integration.repository.ProcessorAuthorizationRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessorClientTest {

    private MockWebServer mockServer;
    private ProcessorClient processorClient;

    @Mock
    private ProcessorAuthorizationRepository authRepo;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        ProcessorProperties props = new ProcessorProperties(
                mockServer.url("/").toString(), 2000, 1);
        processorClient = new ProcessorClient(RestClient.builder(), props, authRepo);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void approve_returnsAuthCode() {
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-1\",\"status\":\"APPROVED\",\"authCode\":\"abc123\"}"));

        ProcessorRequest req = new ProcessorRequest("tx-1", new BigDecimal("100.00"), "USD");
        ProcessorResponse response = processorClient.authorize(UUID.randomUUID(), req, 1);

        assertThat(response.isApproved()).isTrue();
        assertThat(response.authCode()).isEqualTo("abc123");
        verify(authRepo, times(2)).save(any());
    }

    @Test
    void declined_throwsProcessorDeclinedException() {
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-2\",\"status\":\"DECLINED\",\"authCode\":null}"));

        ProcessorRequest req = new ProcessorRequest("tx-2", new BigDecimal("50.00"), "USD");

        assertThatThrownBy(() -> processorClient.authorize(UUID.randomUUID(), req, 1))
                .isInstanceOf(ProcessorDeclinedException.class);
        verify(authRepo, times(2)).save(any());
    }

    @Test
    void timeoutStatus_throwsProcessorTimeoutException() {
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-3\",\"status\":\"TIMEOUT\",\"authCode\":null}"));

        ProcessorRequest req = new ProcessorRequest("tx-3", new BigDecimal("25.00"), "USD");

        assertThatThrownBy(() -> processorClient.authorize(UUID.randomUUID(), req, 1))
                .isInstanceOf(ProcessorTimeoutException.class);
        verify(authRepo, times(2)).save(any());
    }

    @Test
    void networkError_throwsProcessorTimeoutException() throws IOException {
        mockServer.shutdown();

        ProcessorRequest req = new ProcessorRequest("tx-4", new BigDecimal("10.00"), "USD");

        assertThatThrownBy(() -> processorClient.authorize(UUID.randomUUID(), req, 1))
                .isInstanceOf(ProcessorTimeoutException.class);
    }

    @Test
    void retry_succeedsOnSecondAttempt() {
        ProcessorProperties props = new ProcessorProperties(
                mockServer.url("/").toString(), 2000, 2);
        ProcessorClient retryClient = new ProcessorClient(RestClient.builder(), props, authRepo);

        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-5\",\"status\":\"APPROVED\",\"authCode\":\"xyz\"}"));

        ProcessorRequest req = new ProcessorRequest("tx-5", new BigDecimal("100.00"), "USD");
        ProcessorResponse response = retryClient.authorizeWithRetry(UUID.randomUUID(), req);

        assertThat(response.isApproved()).isTrue();
        assertThat(response.authCode()).isEqualTo("xyz");
    }

    @Test
    void retry_exhausted_throwsTimeout() {
        ProcessorProperties props = new ProcessorProperties(
                mockServer.url("/").toString(), 2000, 2);
        ProcessorClient retryClient = new ProcessorClient(RestClient.builder(), props, authRepo);

        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        ProcessorRequest req = new ProcessorRequest("tx-6", new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> retryClient.authorizeWithRetry(UUID.randomUUID(), req))
                .isInstanceOf(ProcessorTimeoutException.class);
    }

    @Test
    void declined_notRetried() {
        ProcessorProperties props = new ProcessorProperties(
                mockServer.url("/").toString(), 2000, 3);
        ProcessorClient retryClient = new ProcessorClient(RestClient.builder(), props, authRepo);

        mockServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"reference\":\"tx-7\",\"status\":\"DECLINED\",\"authCode\":null}"));

        ProcessorRequest req = new ProcessorRequest("tx-7", new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> retryClient.authorizeWithRetry(UUID.randomUUID(), req))
                .isInstanceOf(ProcessorDeclinedException.class);
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }
}
