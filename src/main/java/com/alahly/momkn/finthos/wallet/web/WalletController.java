package com.alahly.momkn.finthos.wallet.web;

import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.wallet.mapper.WalletMapper;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import com.alahly.momkn.finthos.wallet.web.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletMapper walletMapper;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        var wallet = walletService.getByUserId(user.getId());
        return ResponseEntity.ok(walletMapper.toResponse(wallet));
    }
}
