package com.alahly.momkn.finthos.admin.service;

import com.alahly.momkn.finthos.admin.web.dto.AdminUserPage;
import com.alahly.momkn.finthos.admin.web.dto.AdminUserResponse;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final WalletService walletService;

    public AdminUserPage listUsers(int page, int size) {
        Page<User> userPage = userRepository.findAll(PageRequest.of(page, size));
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

        if (original.getStatus() != TxStatus.SUCCESS) {
            throw new IllegalStateException("Only SUCCESS transactions can be reversed");
        }

        String idempotencyKey = "REVERSAL-" + original.getId();
        Transaction reversal = transactionService.create(
                TxType.REVERSAL, original.getAmount(), idempotencyKey,
                original.getTargetWalletId(), original.getSourceWalletId());

        if (original.getSourceWalletId() != null) {
            walletService.credit(original.getSourceWalletId(), original.getAmount(), reversal.getId());
        }
        if (original.getTargetWalletId() != null) {
            walletService.debit(original.getTargetWalletId(), original.getAmount(), reversal.getId());
        }

        transactionService.markSuccess(reversal);

        original.setStatus(TxStatus.REVERSED);
        transactionService.markPersisted(original);
        transactionService.markSuccess(original);

        return reversal;
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
