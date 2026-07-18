package com.alahly.momkn.finthos.integration.repository;

import com.alahly.momkn.finthos.integration.domain.ProcessorAuthorization;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessorAuthorizationRepository extends CrudRepository<ProcessorAuthorization, UUID> {

    @Query("SELECT * FROM processor_authorizations WHERE transaction_id = :transactionId ORDER BY attempt_number")
    List<ProcessorAuthorization> findByTransactionId(UUID transactionId);
}
