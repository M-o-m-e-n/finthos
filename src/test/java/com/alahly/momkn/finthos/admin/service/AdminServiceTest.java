package com.alahly.momkn.finthos.admin.service;

import com.alahly.momkn.finthos.admin.web.dto.AdminUserPage;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
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
        when(userRepository.findAll(PageRequest.of(0, 20))).thenReturn(page);

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

        assertThrows(IllegalStateException.class,
                () -> adminService.reverseTransaction(txId, adminId));
    }

    @Test
    void reverseTransaction_success_createsReversal() {
        UUID txId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID targetWalletId = UUID.randomUUID();

        Transaction original = Transaction.create(TxType.TRANSFER, new BigDecimal("50.00"), "k",
                sourceWalletId, targetWalletId);
        original.markPersisted();
        original.markSuccess();
        when(transactionService.findById(txId)).thenReturn(original);

        Transaction reversal = Transaction.create(TxType.REVERSAL, new BigDecimal("50.00"),
                "REVERSAL-" + txId, targetWalletId, sourceWalletId);
        reversal.markPersisted();
        when(transactionService.create(any(TxType.class), any(BigDecimal.class), any(String.class),
                any(UUID.class), any(UUID.class))).thenReturn(reversal);

        Transaction result = adminService.reverseTransaction(txId, adminId);

        assertThat(result.getType()).isEqualTo(TxType.REVERSAL);
        verify(transactionService).markSuccess(reversal);
    }
}
