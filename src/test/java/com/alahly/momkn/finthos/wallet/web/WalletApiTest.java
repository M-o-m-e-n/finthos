package com.alahly.momkn.finthos.wallet.web;

import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndToken(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(username, email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();
        return token;
    }

    @Test
    void register_createsWalletWithZeroBalance() throws Exception {
        String token = registerAndToken("fr2-user", "fr2@example.com");

        mockMvc.perform(get("/api/wallets/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/wallets/me"))
                .andExpect(status().isForbidden());
    }
}
