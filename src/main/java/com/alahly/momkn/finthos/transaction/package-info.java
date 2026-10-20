/**
 * Transaction records, P2P transfers, history, idempotency (FR-4, FR-6, FR-7).
 */
@ApplicationModule(allowedDependencies = {
        "common",
        "wallet :: service",
        "wallet :: domain"
})
package com.alahly.momkn.finthos.transaction;

import org.springframework.modulith.ApplicationModule;
