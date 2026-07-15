package com.alahly.momkn.finthos.transaction.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table("ledger_entries")
public class LedgerEntry implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("wallet_id")
    private UUID walletId;

    @Column("transaction_id")
    private UUID transactionId;

    @Column("delta")
    private BigDecimal delta;

    @Column("balance_after")
    private BigDecimal balanceAfter;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean newEntity;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public static LedgerEntry create(UUID walletId, UUID transactionId, BigDecimal delta, BigDecimal balanceAfter) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .walletId(walletId)
                .transactionId(transactionId)
                .delta(delta)
                .balanceAfter(balanceAfter)
                .createdAt(Instant.now())
                .newEntity(true)
                .build();
    }
}