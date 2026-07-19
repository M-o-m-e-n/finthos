package com.alahly.momkn.finthos.transaction.repository;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID>, PagingAndSortingRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT * FROM transactions " +
           "WHERE (source_wallet_id = :walletId OR target_wallet_id = :walletId) " +
           "AND type = COALESCE(:type, type) " +
           "AND created_at >= COALESCE(:fromDate, created_at) " +
           "AND created_at <= COALESCE(:toDate, created_at) " +
           "ORDER BY created_at DESC " +
           "LIMIT :limit OFFSET :offset")
    List<Transaction> findByWalletAndFilters(@Param("walletId") UUID walletId,
                                             @Param("type") String type,
                                             @Param("fromDate") Instant fromDate,
                                             @Param("toDate") Instant toDate,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM transactions " +
           "WHERE (source_wallet_id = :walletId OR target_wallet_id = :walletId) " +
           "AND type = COALESCE(:type, type) " +
           "AND created_at >= COALESCE(:fromDate, created_at) " +
           "AND created_at <= COALESCE(:toDate, created_at)")
    long countByWalletAndFilters(@Param("walletId") UUID walletId,
                                  @Param("type") String type,
                                  @Param("fromDate") Instant fromDate,
                                  @Param("toDate") Instant toDate);
}
