package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.transaction.domain.LedgerEntry;
import com.alahly.momkn.finthos.transaction.repository.LedgerRepository;
import com.alahly.momkn.finthos.transaction.web.dto.TransferRequest;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID senderId;
    private UUID receiverId;
    private String senderToken;

    @BeforeEach
    void setUp() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String senderEmail = "ledger-sender-" + unique + "@example.com";
        String receiverEmail = "ledger-receiver-" + unique + "@example.com";

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
    void transfer_createsTwoLedgerEntries() throws Exception {
        UUID senderWalletId = walletRepository.findByUserId(senderId).orElseThrow().getId();
        UUID receiverWalletId = walletRepository.findByUserId(receiverId).orElseThrow().getId();

        String body = objectMapper.writeValueAsString(new TransferRequest(receiverId, new BigDecimal("25.00")));

        MvcResult result = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String txId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        Iterable<LedgerEntry> allEntries = ledgerRepository.findAll();
        List<LedgerEntry> entries = new ArrayList<>();
        allEntries.forEach(entries::add);

        List<LedgerEntry> txEntries = entries.stream()
                .filter(e -> e.getTransactionId().toString().equals(txId))
                .toList();

        assertThat(txEntries).hasSize(2);

        LedgerEntry debitEntry = txEntries.stream()
                .filter(e -> e.getWalletId().equals(senderWalletId))
                .findFirst().orElseThrow();
        assertThat(debitEntry.getDelta()).isEqualByComparingTo(new BigDecimal("-25.00"));

        LedgerEntry creditEntry = txEntries.stream()
                .filter(e -> e.getWalletId().equals(receiverWalletId))
                .findFirst().orElseThrow();
        assertThat(creditEntry.getDelta()).isEqualByComparingTo(new BigDecimal("25.00"));
    }
}
