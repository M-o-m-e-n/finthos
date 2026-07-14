package com.alahly.momkn.finthos.wallet.repository;

import com.alahly.momkn.finthos.wallet.domain.Wallet;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends CrudRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
