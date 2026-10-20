/**
 * Client for the external payment processor (retry + circuit breaker, NFR-6).
 */
@ApplicationModule(allowedDependencies = {
        "common"
})
package com.alahly.momkn.finthos.integration;

import org.springframework.modulith.ApplicationModule;
