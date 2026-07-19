package com.alahly.momkn.finthos.integration.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Builder
@Table("processor_authorizations")
public class ProcessorAuthorization implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID transactionId;
    private String reference;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String authCode;
    private int attemptNumber;
    private int timeoutMs;
    private Instant requestedAt;
    private Instant respondedAt;

    @Transient
    private boolean newEntity;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public static ProcessorAuthorization create(UUID transactionId, String reference,
                                                 BigDecimal amount, String currency,
                                                 int attemptNumber, int timeoutMs) {
        return ProcessorAuthorization.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .reference(reference)
                .amount(amount)
                .currency(currency)
                .status("PENDING")
                .attemptNumber(attemptNumber)
                .timeoutMs(timeoutMs)
                .requestedAt(Instant.now())
                .newEntity(true)
                .build();
    }

    public void markApproved(String authCode) {
        this.status = "APPROVED";
        this.authCode = authCode;
        this.respondedAt = Instant.now();
    }

    public void markDeclined() {
        this.status = "DECLINED";
        this.respondedAt = Instant.now();
    }

    public void markTimeout() {
        this.status = "TIMEOUT";
        this.respondedAt = null;
    }

    public void markPersisted() {
        this.newEntity = false;
    }
}
