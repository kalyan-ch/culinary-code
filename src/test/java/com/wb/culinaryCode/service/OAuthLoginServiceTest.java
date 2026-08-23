package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.RecipeUserRepository;
import com.wb.culinaryCode.model.recipe.RecipeUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.wb.culinaryCode.service.OAuthLoginService.GOOGLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    private static final String SUBJECT = "google-subject-123";

    @Mock
    private RecipeUserRepository users;

    @InjectMocks
    private OAuthLoginService service;

    private static RecipeUser user(String email, String passwordHash) {
        return RecipeUser.builder()
                .id(UUID.randomUUID())
                .email(email)
                .displayName("Alice Baker")
                .passwordHash(passwordHash)
                .build();
    }

    @Test
    @DisplayName("an already-linked provider signs straight in without re-linking")
    void alreadyLinked() {
        var existing = user("alice@example.com", null);
        when(users.findByProviderAndProviderId(GOOGLE, SUBJECT)).thenReturn(Optional.of(existing));

        var result = service.resolve(GOOGLE, SUBJECT, "alice@example.com", "Alice Baker", true);

        assertThat(result.user()).isSameAs(existing);
        assertThat(result.linkedExistingAccount()).isFalse();
        verify(users, never()).save(any(RecipeUser.class));
    }

    @Test
    @DisplayName("a verified email attaches the provider to the existing password account")
    void linksVerifiedEmailToExistingAccount() {
        var existing = user("alice@example.com", "$2a$hash");
        when(users.findByProviderAndProviderId(GOOGLE, SUBJECT)).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(existing));
        when(users.save(any(RecipeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.resolve(GOOGLE, SUBJECT, "alice@example.com", "Alice Baker", true);

        assertThat(result.linkedExistingAccount()).isTrue();
        assertThat(result.user().getProvider()).isEqualTo(GOOGLE);
        assertThat(result.user().getProviderId()).isEqualTo(SUBJECT);
        // the password is untouched, so the account keeps both ways in
        assertThat(result.user().getPasswordHash()).isEqualTo("$2a$hash");
    }

    @Test
    @DisplayName("an unverified email never takes over an existing account")
    void unverifiedEmailDoesNotLink() {
        when(users.findByProviderAndProviderId(GOOGLE, SUBJECT)).thenReturn(Optional.empty());
        when(users.save(any(RecipeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.resolve(GOOGLE, SUBJECT, "alice@example.com", "Alice Baker", false);

        assertThat(result.linkedExistingAccount()).isFalse();
        // the existing account is never even looked up, so it cannot be claimed
        verify(users, never()).findByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("an unknown provider subject creates a passwordless account")
    void createsNewAccount() {
        when(users.findByProviderAndProviderId(GOOGLE, SUBJECT)).thenReturn(Optional.empty());
        // the lookup uses the provider's raw casing; only the stored email is normalised
        when(users.findByEmailIgnoreCase("New@Example.com")).thenReturn(Optional.empty());
        when(users.save(any(RecipeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.resolve(GOOGLE, SUBJECT, "New@Example.com", "New Person", true);

        var saved = ArgumentCaptor.forClass(RecipeUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getValue().getProviderId()).isEqualTo(SUBJECT);
        assertThat(saved.getValue().getPasswordHash()).isNull();
        assertThat(result.linkedExistingAccount()).isFalse();
    }

    @Test
    @DisplayName("a missing display name falls back to the email")
    void fallsBackToEmailForDisplayName() {
        when(users.findByProviderAndProviderId(GOOGLE, SUBJECT)).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(users.save(any(RecipeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.resolve(GOOGLE, SUBJECT, "new@example.com", "  ", true);

        assertThat(result.user().getDisplayName()).isEqualTo("new@example.com");
    }
}
