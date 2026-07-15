package com.alahly.momkn.finthos.transaction.repository;

import com.alahly.momkn.finthos.transaction.domain.LedgerEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface LedgerRepository extends CrudRepository<LedgerEntry, UUID> {
}
