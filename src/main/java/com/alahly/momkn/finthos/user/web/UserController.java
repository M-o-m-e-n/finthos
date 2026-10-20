package com.alahly.momkn.finthos.user.web;

import com.alahly.momkn.finthos.common.security.AuthenticatedUser;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.service.UserService;
import com.alahly.momkn.finthos.user.web.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser me) {
        User user = userService.getById(me.id());

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }
}
