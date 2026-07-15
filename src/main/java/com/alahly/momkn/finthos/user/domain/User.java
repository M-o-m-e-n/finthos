package com.alahly.momkn.finthos.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
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

    @Transient
    private boolean newEntity;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @Builder
    private User(UUID id, String username, String email, String passwordHash,
                 Role role, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.newEntity = true;
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