package com.alahly.momkn.finthos.admin.service;

import com.alahly.momkn.finthos.admin.web.dto.AdminUserPage;
import com.alahly.momkn.finthos.common.error.InvalidTransactionStateException;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.service.UserService;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private WalletService walletService;

    @InjectMocks
    private AdminService adminService;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
    }

    @Test
    void listUsers_returnsPaginatedUsers() {
        User user = User.create("alice", "alice@example.com", "hash", Role.USER);
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(userService.listUsers(PageRequest.of(0, 20))).thenReturn(page);

        AdminUserPage result = adminService.listUsers(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void reverseTransaction_nonSuccess_throws() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.create(TxType.TRANSFER, new BigDecimal("10.00"), "k",
                UUID.randomUUID(), UUID.randomUUID());
        tx.markFailed();
        when(transactionService.findById(txId)).thenReturn(tx);

        assertThrows(InvalidTransactionStateException.class,
                () -> adminService.reverseTransaction(txId, adminId));
    }

    @Test
    void reverseTransaction_alreadyReversed_throws() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.create(TxType.TRANSFER, new BigDecimal("10.00"), "k",
                UUID.randomUUID(), UUID.randomUUID());
        tx.markReversed();
        when(transactionService.findById(txId)).thenReturn(tx);

        assertThrows(InvalidTransactionStateException.class,
                () -> adminService.reverseTransaction(txId, adminId));
        verify(transactionService, never()).createReversal(any(), any());
    }

    @Test
    void reverseTransaction_success_movesFundsBackAndMarksOriginalReversed() {
        UUID txId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID targetWalletId = UUID.randomUUID();

        Transaction original = Transaction.create(TxType.TRANSFER, new BigDecimal("50.00"), "k",
                sourceWalletId, targetWalletId);
        original.markPersisted();
        original.markSuccess();
        when(transactionService.findById(txId)).thenReturn(original);

        Transaction reversal = Transaction.reversalOf(original, adminId);
        reversal.markPersisted();
        when(transactionService.createReversal(original, adminId)).thenReturn(reversal);

        Transaction result = adminService.reverseTransaction(txId, adminId);

        assertThat(result.getType()).isEqualTo(TxType.REVERSAL);
        assertThat(result.getReversedTransactionId()).isEqualTo(original.getId());
        assertThat(result.getReversedByAdminId()).isEqualTo(adminId);
        verify(walletService).debit(targetWalletId, new BigDecimal("50.00"), reversal.getId());
        verify(walletService).credit(sourceWalletId, new BigDecimal("50.00"), reversal.getId());
        verify(transactionService).markSuccess(reversal);
        verify(transactionService).markReversed(original);
        verify(transactionService, never()).markSuccess(original);
    }
}
