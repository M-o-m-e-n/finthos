package com.alahly.momkn.finthos.integration.client;

import java.math.BigDecimal;

public record ProcessorRequest(String reference, BigDecimal amount, String currency) {
}
