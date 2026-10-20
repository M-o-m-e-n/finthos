package com.alahly.momkn.finthos.admin.web;

import java.util.Map;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void reverseTransaction_nonExistent_returns404() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        String token = loginAndGetToken("admin-" + unique + "@example.com");

        mockMvc.perform(post("/api/admin/transactions/" + UUID.randomUUID() + "/reverse")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listUsers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleUserEnabled_admin_disablesUser_returns200() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-toggle-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        User target = User.create("target-" + unique, "target-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(target);

        String token = loginAndGetToken("admin-toggle-" + unique + "@example.com");

        mockMvc.perform(patch("/api/admin/users/" + target.getId() + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void toggleUserEnabled_nonAdmin_returns403() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.create("user-" + unique, "user-toggle-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(user);

        String token = loginAndGetToken("user-toggle-" + unique + "@example.com");

        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleUserEnabled_nonExistentUser_returns404() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-toggle2-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        String token = loginAndGetToken("admin-toggle2-" + unique + "@example.com");

        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_admin_deletesUser_returns204() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-delete-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        User target = User.create("target-" + unique, "target-delete-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(target);

        String token = loginAndGetToken("admin-delete-" + unique + "@example.com");

        mockMvc.perform(delete("/api/admin/users/" + target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_nonAdmin_returns403() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.create("user-" + unique, "user-delete-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(user);

        String token = loginAndGetToken("user-delete-" + unique + "@example.com");

        mockMvc.perform(delete("/api/admin/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_nonExistentUser_returns404() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-delete2-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);

        String token = loginAndGetToken("admin-delete2-" + unique + "@example.com");

        mockMvc.perform(delete("/api/admin/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String loginAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void reverseTransfer_restoresBalancesWithAuditTrail_andCannotBeReversedTwice() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User admin = User.create("admin-" + unique, "admin-rev-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);
        String adminToken = loginAndGetToken("admin-rev-" + unique + "@example.com");

        JsonNode sender = register("rev-sender-" + unique);
        JsonNode receiver = register("rev-receiver-" + unique);
        UUID senderId = UUID.fromString(sender.get("user").get("id").asText());
        UUID receiverId = UUID.fromString(receiver.get("user").get("id").asText());
        jdbcTemplate.update("UPDATE wallets SET balance = 100.00 WHERE user_id = ?", senderId);

        MvcResult transfer = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + sender.get("token").asText())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":\"" + receiverId + "\",\"amount\":40.00}"))
                .andExpect(status().isOk())
                .andReturn();
        String originalId = objectMapper.readTree(transfer.getResponse().getContentAsString()).get("id").asText();

        MvcResult reversal = mockMvc.perform(post("/api/admin/transactions/" + originalId + "/reverse")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REVERSAL"))
                .andReturn();
        UUID reversalId = UUID.fromString(objectMapper.readTree(reversal.getResponse().getContentAsString()).get("id").asText());

        assertThat(balanceOf(senderId)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(receiverId)).isEqualByComparingTo("0");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM transactions WHERE id = ?",
                String.class, UUID.fromString(originalId))).isEqualTo("REVERSED");
        Map<String, Object> reversalRow = jdbcTemplate.queryForMap(
                "SELECT reversed_transaction_id, reversed_by_admin_id FROM transactions WHERE id = ?", reversalId);
        assertThat(reversalRow.get("reversed_transaction_id")).isEqualTo(UUID.fromString(originalId));
        assertThat(reversalRow.get("reversed_by_admin_id")).isEqualTo(admin.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries WHERE transaction_id = ?",
                Integer.class, reversalId)).isEqualTo(2);

        mockMvc.perform(post("/api/admin/transactions/" + originalId + "/reverse")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE"));

        mockMvc.perform(delete("/api/admin/users/" + senderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    private JsonNode register(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"email\":\"" + name
                                + "@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private BigDecimal balanceOf(UUID userId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
    }
}
