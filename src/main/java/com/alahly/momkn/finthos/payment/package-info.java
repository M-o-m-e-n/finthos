/**
 * Processor-backed flows: top-up and merchant payment (FR-3, FR-5).
 */
@ApplicationModule(allowedDependencies = {
        "common",
        "user :: service",
        "wallet :: service",
        "wallet :: domain",
        "transaction :: service",
        "transaction :: domain",
        "integration :: client"
})
package com.alahly.momkn.finthos.payment;

import org.springframework.modulith.ApplicationModule;
