package com.alahly.momkn.finthos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Enforces module boundaries (NFR-5): each top-level package is a module, its
 * subpackages are internal unless exposed via {@code @NamedInterface}.
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(FinthosApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        MODULES.verify();
    }

    @Test
    void writesModuleDocumentation() {
        // Generates PlantUML component diagrams + module canvases under target/spring-modulith-docs
        new Documenter(MODULES).writeDocumentation();
    }
}
