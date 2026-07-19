package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WalletPerformanceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getBalance_completesWithin300ms() {
        Wallet wallet = createTestWallet();
        long start = System.currentTimeMillis();
        walletService.getBalance(wallet.getId());
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isLessThan(300);
    }

    @Test
    void getByUserId_completesWithin300ms() {
        User user = createTestUser();
        walletRepository.save(Wallet.create(user.getId(), "EGP"));
        long start = System.currentTimeMillis();
        walletService.getByUserId(user.getId());
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isLessThan(300);
    }

    private Wallet createTestWallet() {
        User user = createTestUser();
        return walletRepository.save(Wallet.create(user.getId(), "EGP"));
    }

    private User createTestUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.create("perf-" + unique, "perf-" + unique + "@example.com",
                passwordEncoder.encode("password123"), Role.USER);
        return userRepository.save(user);
    }
}
