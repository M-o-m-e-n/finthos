package com.alahly.momkn.finthos.transaction.web;

import com.alahly.momkn.finthos.transaction.web.dto.TransferRequest;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferApiTest {

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
        String senderEmail = "tx-sender-" + unique + "@example.com";
        String receiverEmail = "tx-receiver-" + unique + "@example.com";

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
        String receiverBody = receiverResult.getResponse().getContentAsString();
        receiverId = UUID.fromString(objectMapper.readTree(receiverBody).get("user").get("id").asText());

        UUID senderWalletId = walletRepository.findByUserId(senderId).orElseThrow().getId();
        jdbcTemplate.update("UPDATE wallets SET balance = balance + 100.00 WHERE id = ?", senderWalletId);
    }

    @Test
    void transfer_success_decreasesSenderIncreasesReceiver() throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(receiverId, new BigDecimal("30.00")));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(30.00));

        BigDecimal senderBalance = walletRepository.findByUserId(senderId).orElseThrow().getBalance();
        BigDecimal receiverBalance = walletRepository.findByUserId(receiverId).orElseThrow().getBalance();
        assertThat(senderBalance).isEqualByComparingTo("70.00");
        assertThat(receiverBalance).isEqualByComparingTo("30.00");
    }

    @Test
    void transfer_insufficientFunds_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(receiverId, new BigDecimal("200.00")));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void transfer_toSelf_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(senderId, new BigDecimal("10.00")));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void concurrentTransfers_optimisticLockPreventsLostUpdate() throws Exception {
        UUID senderWalletId = walletRepository.findByUserId(senderId).orElseThrow().getId();
        jdbcTemplate.update("UPDATE wallets SET balance = balance + 100.00 WHERE id = ?", senderWalletId);
        BigDecimal startBalance = walletRepository.findByUserId(senderId).orElseThrow().getBalance();

        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("15.00");
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                String transferBody = objectMapper.writeValueAsString(new TransferRequest(receiverId, transferAmount));
                MvcResult result = mockMvc.perform(post("/api/transfers")
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody))
                        .andReturn();
                return result.getResponse().getStatus();
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        long successCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return -1;
                    }
                })
                .filter(s -> s == 200)
                .count();

        BigDecimal finalSenderBalance = walletRepository.findByUserId(senderId).orElseThrow().getBalance();
        BigDecimal expectedDebit = transferAmount.multiply(BigDecimal.valueOf(successCount));

        assertThat(successCount).isGreaterThanOrEqualTo(1);
        assertThat(successCount).isLessThanOrEqualTo(threadCount);
        assertThat(finalSenderBalance).isEqualByComparingTo(startBalance.subtract(expectedDebit));
    }
}
