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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionHistoryApiTest {

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
        String senderEmail = "hist-sender-" + unique + "@example.com";
        String receiverEmail = "hist-receiver-" + unique + "@example.com";

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
        jdbcTemplate.update("UPDATE wallets SET balance = balance + 500.00 WHERE id = ?", senderWalletId);
    }

    private void doTransfer(BigDecimal amount) throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(receiverId, amount));
        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void listTransactions_returnsContent() throws Exception {
        doTransfer(new BigDecimal("10.00"));
        doTransfer(new BigDecimal("20.00"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listTransactions_emptyWallet_returnsEmptyPage() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest req = new RegisterRequest("empty-" + unique, "empty-" + unique + "@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listTransactions_filterByType_returnsOnlyMatching() throws Exception {
        doTransfer(new BigDecimal("10.00"));
        doTransfer(new BigDecimal("20.00"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("type", "TRANSFER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("type", "TOP_UP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listTransactions_pagination_respectsPageAndSize() throws Exception {
        doTransfer(new BigDecimal("10.00"));
        doTransfer(new BigDecimal("20.00"));
        doTransfer(new BigDecimal("30.00"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void listTransactions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());
    }
}
