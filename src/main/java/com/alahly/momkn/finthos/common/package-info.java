/**
 * Shared kernel: error model, security (JWT, {@code AuthenticatedUser}), correlation ids,
 * logging. OPEN so every module may use it; it must not depend on any other module.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.alahly.momkn.finthos.common;

import org.springframework.modulith.ApplicationModule;
