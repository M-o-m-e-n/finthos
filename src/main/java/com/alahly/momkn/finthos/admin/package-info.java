/**
 * Admin user management and transaction reversal (FR-8).
 */
@ApplicationModule(allowedDependencies = {
        "common",
        "user :: service",
        "user :: domain",
        "wallet :: service",
        "wallet :: domain",
        "transaction :: service",
        "transaction :: domain"
})
package com.alahly.momkn.finthos.admin;

import org.springframework.modulith.ApplicationModule;
