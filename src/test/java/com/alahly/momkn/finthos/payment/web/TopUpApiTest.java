package com.alahly.momkn.finthos.payment.web;

import com.alahly.momkn.finthos.payment.web.dto.TopUpRequest;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.alahly.momkn.finthos.integration.client.ProcessorClient;
import com.alahly.momkn.finthos.integration.client.ProcessorRequest;
import com.alahly.momkn.finthos.integration.client.ProcessorResponse;
import com.alahly.momkn.finthos.common.error.ProcessorTimeoutException;
import com.alahly.momkn.finthos.common.error.ProcessorDeclinedException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TopUpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private ProcessorClient processorClient;

    private UUID userId;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "topup-user-" + unique + "@example.com";

        RegisterRequest req = new RegisterRequest("topup-" + unique, email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        userToken = objectMapper.readTree(body).get("token").asText();
        userId = UUID.fromString(objectMapper.readTree(body).get("user").get("id").asText());
    }

    @Test
    void topUp_approved_creditsWallet() throws Exception {
        when(processorClient.authorizeWithRetry(any(UUID.class), any(ProcessorRequest.class)))
                .thenReturn(new ProcessorResponse("ref", "APPROVED", "AUTH-001"));

        TopUpRequest req = new TopUpRequest(new BigDecimal("50.00"));
        mockMvc.perform(post("/api/wallets/topup")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.type").value("TOP_UP"))
                .andExpect(jsonPath("$.walletBalance").value(50.00));

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void topUp_declined_returns402() throws Exception {
        when(processorClient.authorizeWithRetry(any(UUID.class), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorDeclinedException("ref"));

        TopUpRequest req = new TopUpRequest(new BigDecimal("50.00"));
        mockMvc.perform(post("/api/wallets/topup")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("PROCESSOR_DECLINED"));

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void topUp_timeout_returns504() throws Exception {
        when(processorClient.authorizeWithRetry(any(UUID.class), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorTimeoutException("ref"));

        TopUpRequest req = new TopUpRequest(new BigDecimal("50.00"));
        mockMvc.perform(post("/api/wallets/topup")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("PROCESSOR_TIMEOUT"));

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void topUp_unauthenticated_returns403() throws Exception {
        TopUpRequest req = new TopUpRequest(new BigDecimal("50.00"));
        mockMvc.perform(post("/api/wallets/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
