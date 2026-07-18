package com.alahly.momkn.finthos.payment.web;

import com.alahly.momkn.finthos.payment.web.dto.PaymentRequest;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
class PaymentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ProcessorClient processorClient;

    private UUID userId;
    private UUID merchantId;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String userEmail = "payer-" + unique + "@example.com";
        String merchantEmail = "merchant-" + unique + "@example.com";

        RegisterRequest userReq = new RegisterRequest("payer-" + unique, userEmail, "password123");
        MvcResult userResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String userBody = userResult.getResponse().getContentAsString();
        userToken = objectMapper.readTree(userBody).get("token").asText();
        userId = UUID.fromString(objectMapper.readTree(userBody).get("user").get("id").asText());

        RegisterRequest merchantReq = new RegisterRequest("merchant-" + unique, merchantEmail, "password123");
        MvcResult merchantResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String merchantBody = merchantResult.getResponse().getContentAsString();
        merchantId = UUID.fromString(objectMapper.readTree(merchantBody).get("user").get("id").asText());

        Wallet payerWallet = walletRepository.findByUserId(userId).orElseThrow();
        jdbcTemplate.update("UPDATE wallets SET balance = balance + 100.00 WHERE id = ?", payerWallet.getId());
    }

    @Test
    void pay_approved_debitsSenderCreditsMerchant() throws Exception {
        when(processorClient.authorizeWithRetry(any(UUID.class), any(ProcessorRequest.class)))
                .thenReturn(new ProcessorResponse("ref", "APPROVED", "AUTH-001"));

        PaymentRequest req = new PaymentRequest(merchantId, new BigDecimal("30.00"));
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.type").value("PAYMENT"));

        Wallet payerWallet = walletRepository.findByUserId(userId).orElseThrow();
        Wallet merchantWallet = walletRepository.findByUserId(merchantId).orElseThrow();
        assertThat(payerWallet.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(merchantWallet.getBalance()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void pay_declined_returns402() throws Exception {
        when(processorClient.authorizeWithRetry(any(UUID.class), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorDeclinedException("ref"));

        PaymentRequest req = new PaymentRequest(merchantId, new BigDecimal("30.00"));
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("PROCESSOR_DECLINED"));

        Wallet payerWallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(payerWallet.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void pay_timeout_returns504() throws Exception {
        when(processorClient.authorizeWithRetry(any(UUID.class), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorTimeoutException("ref"));

        PaymentRequest req = new PaymentRequest(merchantId, new BigDecimal("30.00"));
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("PROCESSOR_TIMEOUT"));

        Wallet payerWallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(payerWallet.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void pay_unauthenticated_returns403() throws Exception {
        PaymentRequest req = new PaymentRequest(merchantId, new BigDecimal("30.00"));
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
