package com.alahly.momkn.finthos.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "processor")
public record ProcessorProperties(String baseUrl, int timeoutMs, int retryCount) {
}
