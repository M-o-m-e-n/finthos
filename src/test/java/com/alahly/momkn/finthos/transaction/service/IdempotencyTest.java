package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import com.alahly.momkn.finthos.transaction.web.dto.TransferRequest;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID senderId;
    private UUID receiverId;
    private String senderToken;

    @BeforeEach
    void setUp() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String senderEmail = "idem-sender-" + unique + "@example.com";
        String receiverEmail = "idem-receiver-" + unique + "@example.com";

        RegisterRequest senderReq = new RegisterRequest("sender-" + unique, senderEmail, "password123");
        MvcResult senderResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(senderReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String senderBody = senderResult.getResponse().getContentAsString();
        senderToken = objectMapper.readTree(senderBody).get("token").asText();
        senderId = UUID.fromString(objectMapper.readTree(senderBody).get("user").get("id").asText());

        RegisterRequest receiverReq = new RegisterRequest("receiver-" + unique, receiverEmail, "password123");
        MvcResult receiverResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiverReq)))
                .andExpect(status().isCreated())
                .andReturn();
        receiverId = UUID.fromString(objectMapper.readTree(receiverResult.getResponse().getContentAsString()).get("user").get("id").asText());

        UUID senderWalletId = walletRepository.findByUserId(senderId).orElseThrow().getId();
        jdbcTemplate.update("UPDATE wallets SET balance = balance + 100.00 WHERE id = ?", senderWalletId);
    }

    @Test
    void duplicateIdempotencyKey_returnsCachedResult() throws Exception {
        String idemKey = UUID.randomUUID().toString();

        String body = objectMapper.writeValueAsString(
                new TransferRequest(receiverId, new BigDecimal("10.00")));

        MvcResult first = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String secondId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText();
        assertThat(firstId).isEqualTo(secondId);

        Wallet senderAfter = walletRepository.findByUserId(senderId).orElseThrow();
        assertThat(senderAfter.getBalance()).isEqualByComparingTo(new BigDecimal("90.00"));
    }
}
