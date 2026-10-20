package com.alahly.momkn.finthos.transaction.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table("transactions")
public class Transaction implements Persistable<UUID> {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column("type")
    private TxType type;

    @Column("amount")
    private BigDecimal amount;

    @Column("status")
    private TxStatus status;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("source_wallet_id")
    private UUID sourceWalletId;

    @Column("target_wallet_id")
    private UUID targetWalletId;

    @Column("reversed_transaction_id")
    private UUID reversedTransactionId;

    @Column("reversed_by_admin_id")
    private UUID reversedByAdminId;

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

    public void markSuccess() {
        this.status = TxStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = TxStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void markReversed() {
        this.status = TxStatus.REVERSED;
        this.updatedAt = Instant.now();
    }

    public boolean isReversible() {
        return status == TxStatus.SUCCESS && type != TxType.REVERSAL;
    }

    public void markPersisted() {
        this.newEntity = false;
    }

    public static Transaction create(TxType type, BigDecimal amount, String idempotencyKey,
                                     UUID sourceWalletId, UUID targetWalletId) {
        Instant now = Instant.now();
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(type)
                .amount(amount)
                .status(TxStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .sourceWalletId(sourceWalletId)
                .targetWalletId(targetWalletId)
                .createdAt(now)
                .updatedAt(now)
                .newEntity(true)
                .build();
    }

    /** Compensating transaction: moves the original amount back from target to source. */
    public static Transaction reversalOf(Transaction original, UUID adminId) {
        Transaction reversal = create(TxType.REVERSAL, original.getAmount(), "REVERSAL-" + original.getId(),
                original.getTargetWalletId(), original.getSourceWalletId());
        reversal.reversedTransactionId = original.getId();
        reversal.reversedByAdminId = adminId;
        return reversal;
    }
}
