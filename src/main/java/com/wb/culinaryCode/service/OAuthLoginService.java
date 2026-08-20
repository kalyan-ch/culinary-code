package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.RecipeUserRepository;
import com.wb.culinaryCode.model.recipe.RecipeUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    public static final String GOOGLE = "google";

    private final RecipeUserRepository users;

    /** Whether an existing password account had the provider attached to it during sign-in. */
    public record Result(RecipeUser user, boolean linkedExistingAccount) {
    }

    /**
     * Resolves the account behind a provider sign-in, in three steps:
     *
     * <ol>
     *   <li>a previous sign-in with the same provider subject — just log in;</li>
     *   <li>an existing account with the same email — attach the provider and log in, but only
     *       when the provider asserts the email is verified. Skipping that check would let
     *       anyone who can make a provider claim an unverified address take over the account;</li>
     *   <li>otherwise a brand new account with no password.</li>
     * </ol>
     */
    @Transactional
    public Result resolve(String provider, String subject, String email, String displayName,
                          boolean emailVerified) {
        var linked = users.findByProviderAndProviderId(provider, subject);
        if (linked.isPresent()) {
            return new Result(linked.get(), false);
        }

        var byEmail = emailVerified ? users.findByEmailIgnoreCase(email) : java.util.Optional.<RecipeUser>empty();
        if (byEmail.isPresent()) {
            var user = byEmail.get();
            user.setProvider(provider);
            user.setProviderId(subject);
            log.info("Linked {} sign-in to existing account {}", provider, user.getId());
            return new Result(users.save(user), true);
        }

        var created = users.save(RecipeUser.builder()
                .email(email.toLowerCase())
                .displayName(displayName != null && !displayName.isBlank() ? displayName : email)
                .provider(provider)
                .providerId(subject)
                .build());
        return new Result(created, false);
    }
}
