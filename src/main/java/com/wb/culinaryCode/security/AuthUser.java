package com.wb.culinaryCode.security;

import com.wb.culinaryCode.model.recipe.RecipeUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The authenticated principal. Carries everything the API needs about the signed-in user so
 * ownership checks and {@code /me} don't hit the database again. No roles — every account has
 * identical privileges, and authorization here is ownership, not role membership.
 */
public record AuthUser(UUID id, String email, String displayName, String passwordHash)
        implements UserDetails {

    public static AuthUser of(RecipeUser user) {
        return new AuthUser(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
