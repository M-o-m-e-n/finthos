package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.transaction.domain.LedgerEntry;
import com.alahly.momkn.finthos.transaction.repository.LedgerRepository;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.create(UUID.randomUUID(), "USD");
        wallet.credit(new BigDecimal("100.00"));
    }

    @Test
    void credit_increasesBalanceAndCreatesLedgerEntry() {
        UUID txId = UUID.randomUUID();
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.credit(wallet.getId(), new BigDecimal("50.00"), txId);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(walletRepository).save(wallet);
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    void debit_decreasesBalanceAndCreatesLedgerEntry() {
        UUID txId = UUID.randomUUID();
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.debit(wallet.getId(), new BigDecimal("30.00"), txId);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        verify(walletRepository).save(wallet);
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    void debit_insufficientFunds_throwsAndDoesNotSave() {
        UUID txId = UUID.randomUUID();
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertThrows(com.alahly.momkn.finthos.wallet.domain.InsufficientFundsException.class,
                () -> walletService.debit(wallet.getId(), new BigDecimal("200.00"), txId));

        verify(walletRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void getByUserId_throwsWhenNoWallet() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> walletService.getByUserId(userId));
    }

    @Test
    void getByUserId_returnsWallet() {
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getByUserId(wallet.getUserId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(wallet.getId());
    }

    @Test
    void createForUser_savesNewWallet() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.createForUser(userId, "USD");

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getCurrency()).isEqualTo("USD");
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void getBalance_throwsWhenWalletNotFound() {
        when(walletRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> walletService.getBalance(UUID.randomUUID()));
    }

    @Test
    void getBalance_returnsBalance() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(wallet.getId());

        assertThat(balance).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
