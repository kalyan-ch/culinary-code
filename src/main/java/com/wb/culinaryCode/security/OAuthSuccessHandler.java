package com.wb.culinaryCode.security;

import com.wb.culinaryCode.service.OAuthLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Swaps the OIDC principal for the application's own {@link AuthUser} once Google has
 * authenticated someone, so that {@code @AuthenticationPrincipal AuthUser} resolves the same
 * way regardless of how the session started. Without this every controller would have to
 * handle two principal types.
 */
@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthLoginService oauthLogin;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var oidc = (OidcUser) authentication.getPrincipal();

        var result = oauthLogin.resolve(
                OAuthLoginService.GOOGLE,
                oidc.getSubject(),
                oidc.getEmail(),
                oidc.getFullName(),
                Boolean.TRUE.equals(oidc.getEmailVerified()));

        var principal = AuthUser.of(result.user());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        // Linking an existing account is never silent — the UI shows a notice, because someone
        // who forgot they had an account needs to know why their old recipes just appeared.
        response.sendRedirect(frontendUrl + (result.linkedExistingAccount() ? "/?linked=google" : "/"));
    }
}
