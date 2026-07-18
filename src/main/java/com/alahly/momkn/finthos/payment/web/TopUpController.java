package com.alahly.momkn.finthos.payment.web;

import com.alahly.momkn.finthos.payment.service.PaymentService;
import com.alahly.momkn.finthos.payment.web.dto.TopUpRequest;
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
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class TopUpController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final WalletService walletService;

    @PostMapping("/topup")
    public ResponseEntity<TxResult> topUp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();

        Transaction tx = paymentService.topUp(user.getId(), request.getAmount(), key);

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
