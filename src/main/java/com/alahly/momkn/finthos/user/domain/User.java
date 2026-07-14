package com.alahly.momkn.finthos.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private UUID id;
    private String username;
    private String email;
    @Column("password_hash")
    private String passwordHash;
    private Role role;
    private boolean enabled;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;

    @Override
    public boolean isNew() {
        return true;
    }

    public static User create(String username, String email, String passwordHash, Role role) {
        Instant now = Instant.now();
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(role)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
