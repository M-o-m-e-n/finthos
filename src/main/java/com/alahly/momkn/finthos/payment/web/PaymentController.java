package com.alahly.momkn.finthos.payment.web;

import com.alahly.momkn.finthos.payment.service.PaymentService;
import com.alahly.momkn.finthos.payment.web.dto.PaymentRequest;
import com.alahly.momkn.finthos.payment.web.dto.TxResult;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<TxResult> payMerchant(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();

        Transaction tx = paymentService.payMerchant(
                user.getId(), request.getMerchantId(), request.getAmount(), key);

        Wallet wallet = walletService.getByUserId(user.getId());

        TxResult result = TxResult.builder()
                .transactionId(tx.getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .status(tx.getStatus().name())
                .sourceWalletId(tx.getSourceWalletId())
                .targetWalletId(tx.getTargetWalletId())
                .createdAt(tx.getCreatedAt())
                .walletBalance(wallet.getBalance())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
