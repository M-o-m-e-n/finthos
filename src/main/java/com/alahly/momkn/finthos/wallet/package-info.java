/**
 * Balances and their ledger (NFR-3). Creates a wallet for every registered user.
 */
@ApplicationModule(allowedDependencies = {
        "common",
        "user"
})
package com.alahly.momkn.finthos.wallet;

import org.springframework.modulith.ApplicationModule;
