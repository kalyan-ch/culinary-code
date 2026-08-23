package com.wb.culinaryCode.model.auth;

import com.wb.culinaryCode.security.AuthUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request and response payloads for the auth endpoints. Grouped in one file because they
 * are small, always change together, and are only used by {@code AuthController}.
 */
public final class AuthDTOs {

    private AuthDTOs() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank String displayName,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserDTO(UUID id, String email, String displayName) {

        public static UserDTO of(AuthUser user) {
            return new UserDTO(user.id(), user.email(), user.displayName());
        }
    }
}
