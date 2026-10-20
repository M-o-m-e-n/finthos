package com.alahly.momkn.finthos.user;

import java.util.UUID;

/** Published inside the registration transaction, after the user row is saved. */
public record UserRegistered(UUID userId) {
}
