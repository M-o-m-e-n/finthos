package com.alahly.momkn.finthos.admin.web;

import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
    }

    @Test
    void listUsers_admin_returns200() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        String token = loginAndGetToken("admin-" + unique + "@example.com");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listUsers_nonAdmin_returns403() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.create("user-" + unique, "user-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(user);

        String token = loginAndGetToken("user-" + unique + "@example.com");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void reverseTransaction_nonAdmin_returns403() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.create("user-" + unique, "user-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(user);

        String token = loginAndGetToken("user-" + unique + "@example.com");

        mockMvc.perform(post("/api/admin/transactions/" + UUID.randomUUID() + "/reverse")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void reverseTransaction_nonExistent_returns400() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        String token = loginAndGetToken("admin-" + unique + "@example.com");

        mockMvc.perform(post("/api/admin/transactions/" + UUID.randomUUID() + "/reverse")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    private String loginAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
