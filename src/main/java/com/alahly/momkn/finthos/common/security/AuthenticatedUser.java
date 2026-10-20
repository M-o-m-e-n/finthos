package com.alahly.momkn.finthos.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The authenticated caller. Carries the user id so controllers in any module can act on
 * "the current user" without depending on the user module or re-querying it.
 */
public record AuthenticatedUser(UUID id, String email, String passwordHash, String role, boolean enabled)
        implements UserDetails {

    public static final String ADMIN = "ADMIN";

    public boolean isAdmin() {
        return ADMIN.equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", email=" + email + ", role=" + role + ", enabled=" + enabled + "]";
    }
}
