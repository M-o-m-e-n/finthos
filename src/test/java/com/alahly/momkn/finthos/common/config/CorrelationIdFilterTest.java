package com.alahly.momkn.finthos.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsXCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/api/wallets/me")
                        .header("Authorization", "Bearer dummy"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void usesProvidedCorrelationId() throws Exception {
        mockMvc.perform(get("/api/wallets/me")
                        .header("Authorization", "Bearer dummy")
                        .header("X-Correlation-Id", "my-custom-id-123"))
                .andExpect(header().string("X-Correlation-Id", "my-custom-id-123"));
    }
}
