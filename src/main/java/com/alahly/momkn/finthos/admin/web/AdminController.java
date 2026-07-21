package com.alahly.momkn.finthos.admin.web;

import com.alahly.momkn.finthos.admin.service.AdminService;
import com.alahly.momkn.finthos.admin.web.dto.AdminUserPage;
import com.alahly.momkn.finthos.admin.web.dto.AdminUserResponse;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.web.dto.TxResponse;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<AdminUserPage> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.listUsers(page, size));
    }

    @PostMapping("/transactions/{id}/reverse")
    public ResponseEntity<TxResponse> reverseTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Transaction reversal = adminService.reverseTransaction(id, admin.getId());

        TxResponse response = TxResponse.builder()
                .id(reversal.getId())
                .type(reversal.getType().name())
                .amount(reversal.getAmount())
                .status(reversal.getStatus().name())
                .sourceWalletId(reversal.getSourceWalletId())
                .targetWalletId(reversal.getTargetWalletId())
                .createdAt(reversal.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/disable")
    public ResponseEntity<AdminUserResponse> toggleUserEnabled(@PathVariable UUID id) {
        AdminUserResponse updated = adminService.toggleUserEnabled(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
