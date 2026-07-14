package com.alahly.momkn.finthos.wallet.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("wallets")
public class Wallet implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    private BigDecimal balance;

    private String currency;

    @Version
    private Long version;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Override
    public boolean isNew() {
        return true;
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        if (this.balance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public static Wallet create(UUID userId, String currency) {
        Instant now = Instant.now();
        return Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
