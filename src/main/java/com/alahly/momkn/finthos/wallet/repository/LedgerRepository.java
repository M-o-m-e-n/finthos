package com.alahly.momkn.finthos.wallet.repository;

import com.alahly.momkn.finthos.wallet.domain.LedgerEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface LedgerRepository extends CrudRepository<LedgerEntry, UUID> {
}
