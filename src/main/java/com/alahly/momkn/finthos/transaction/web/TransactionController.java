package com.alahly.momkn.finthos.transaction.web;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.transaction.service.TransferService;
import com.alahly.momkn.finthos.transaction.web.dto.TransactionItem;
import com.alahly.momkn.finthos.transaction.web.dto.TransactionPage;
import com.alahly.momkn.finthos.transaction.web.dto.TransferRequest;
import com.alahly.momkn.finthos.transaction.web.dto.TxResponse;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransferService transferService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @PostMapping("/transfers")
    public ResponseEntity<TxResponse> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        User sender = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();

        Transaction tx = transferService.transfer(
                sender.getId(),
                request.getToUserId(),
                request.getAmount(),
                key);

        TxResponse response = TxResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .status(tx.getStatus().name())
                .sourceWalletId(tx.getSourceWalletId())
                .targetWalletId(tx.getTargetWalletId())
                .createdAt(tx.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<TransactionPage> listTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) TxType type,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        Instant fromDate = (from != null) ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toDate = (to != null) ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1) : null;

        PageRequest pageRequest = PageRequest.of(page, size);

        Page<Transaction> txPage = transactionService.listTransactions(
                wallet.getId(), type, fromDate, toDate, pageRequest);

        List<TransactionItem> items = txPage.getContent().stream()
                .map(tx -> TransactionItem.builder()
                        .id(tx.getId())
                        .type(tx.getType().name())
                        .amount(tx.getAmount())
                        .status(tx.getStatus().name())
                        .sourceWalletId(tx.getSourceWalletId())
                        .targetWalletId(tx.getTargetWalletId())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();

        TransactionPage response = TransactionPage.builder()
                .content(items)
                .page(txPage.getNumber())
                .size(txPage.getSize())
                .totalElements(txPage.getTotalElements())
                .totalPages(txPage.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionItem> getTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        Transaction tx = transactionService.findById(id);

        TransactionItem response = TransactionItem.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .status(tx.getStatus().name())
                .sourceWalletId(tx.getSourceWalletId())
                .targetWalletId(tx.getTargetWalletId())
                .createdAt(tx.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}
