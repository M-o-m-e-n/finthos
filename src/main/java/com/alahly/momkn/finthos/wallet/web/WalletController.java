package com.alahly.momkn.finthos.wallet.web;

import com.alahly.momkn.finthos.common.security.AuthenticatedUser;
import com.alahly.momkn.finthos.wallet.mapper.WalletMapper;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import com.alahly.momkn.finthos.wallet.web.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletMapper walletMapper;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal AuthenticatedUser me) {
        var wallet = walletService.getByUserId(me.id());
        return ResponseEntity.ok(walletMapper.toResponse(wallet));
    }
}
