package com.alahly.momkn.finthos.transaction.web;

import com.alahly.momkn.finthos.common.security.AuthenticatedUser;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.transaction.service.TransferService;
import com.alahly.momkn.finthos.transaction.web.dto.TransactionItem;
import com.alahly.momkn.finthos.transaction.web.dto.TransactionPage;
import com.alahly.momkn.finthos.transaction.web.dto.TransferRequest;
import com.alahly.momkn.finthos.transaction.web.dto.TxResponse;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final WalletService walletService;

    @PostMapping("/transfers")
    public ResponseEntity<TxResponse> transfer(
            @AuthenticationPrincipal AuthenticatedUser me,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        Transaction tx = transferService.transfer(
                me.id(),
                request.getToUserId(),
                request.getAmount(),
                idempotencyKey);

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
            @AuthenticationPrincipal AuthenticatedUser me,
            @RequestParam(required = false) TxType type,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Wallet wallet = walletService.getByUserId(me.id());

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
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id) {

        Wallet wallet = walletService.getByUserId(me.id());

        Transaction tx = transactionService.getVisibleTo(id, wallet.getId(), me.isAdmin());

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
