package com.alahly.momkn.finthos.admin.service;

import com.alahly.momkn.finthos.admin.web.dto.AdminUserPage;
import com.alahly.momkn.finthos.admin.web.dto.AdminUserResponse;
import com.alahly.momkn.finthos.common.error.InvalidTransactionStateException;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.service.UserService;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserService userService;
    private final TransactionService transactionService;
    private final WalletService walletService;

    public AdminUserPage listUsers(int page, int size) {
        Page<User> userPage = userService.listUsers(PageRequest.of(page, size));
        List<AdminUserResponse> content = userPage.getContent().stream()
                .map(this::toAdminUser)
                .toList();
        return AdminUserPage.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    @Transactional
    public Transaction reverseTransaction(UUID transactionId, UUID adminId) {
        Transaction original = transactionService.findById(transactionId);

        if (!original.isReversible()) {
            throw new InvalidTransactionStateException(
                    "Only SUCCESS, non-reversal transactions can be reversed (status: " + original.getStatus() + ")");
        }

        Transaction reversal = transactionService.createReversal(original, adminId);

        // Debit first: if the target has already spent the money this fails before anything is credited.
        if (original.getTargetWalletId() != null) {
            walletService.debit(original.getTargetWalletId(), original.getAmount(), reversal.getId());
        }
        if (original.getSourceWalletId() != null) {
            walletService.credit(original.getSourceWalletId(), original.getAmount(), reversal.getId());
        }

        transactionService.markSuccess(reversal);
        transactionService.markReversed(original);

        return reversal;
    }

    public AdminUserResponse toggleUserEnabled(UUID userId) {
        return toAdminUser(userService.toggleEnabled(userId));
    }

    /**
     * Hard delete is only allowed for users with no money history; anything else would destroy
     * the audit trail. Such users should be disabled instead.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        userService.getById(userId);
        walletService.findByUserId(userId).ifPresent(wallet -> {
            if (transactionService.hasTransactions(wallet.getId())) {
                throw new InvalidTransactionStateException(
                        "User has transaction history and cannot be deleted; disable the account instead");
            }
        });
        userService.delete(userId);
    }

    private AdminUserResponse toAdminUser(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
