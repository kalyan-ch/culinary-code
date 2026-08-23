package com.wb.culinaryCode.config;

import com.wb.culinaryCode.dao.RecipeUserRepository;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.security.OAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RecipeUserRepository users;
    private final OAuthSuccessHandler oauthSuccessHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> users.findByEmailIgnoreCase(email)
                .map(AuthUser::of)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF tokens are off in favour of SameSite=Lax on the session cookie (set in
                // application.yml). Lax stops the browser sending the cookie on cross-site
                // POST/PUT/DELETE, and no state-changing endpoint here answers GET, so the
                // cross-site request forgery path is closed. Re-enable tokens if the API ever
                // needs to be called from a different origin than the one that logged in.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/oauth2/**", "/api/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Browsing is open so a signed-out visitor can find published recipes.
                        // The service still decides what each viewer may see: with no session
                        // that is published recipes only. Creating and editing stay closed.
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipe", "/api/v1/recipe/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags").permitAll()
                        .anyRequest().authenticated())
                // Without this the default entry point answers an unauthenticated API call with
                // a redirect to a login page that doesn't exist, which the frontend sees as an
                // opaque 200 of HTML rather than a 401.
                // Spring saves the attempted request into a new session so it can replay it
                // after a form login, which handed a JSESSIONID to visitors who had never
                // signed in — enough to fool any "is the cookie there?" check. This API
                // answers 401 instead of redirecting, so the saved request is never used.
                .requestCache(cache -> cache.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, failure) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Not signed in\"}");
                }))
                // Both endpoints live under /api so the frontend's same-origin proxy covers the
                // whole redirect dance; otherwise the callback would set a third-party cookie.
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(a -> a.baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(r -> r.baseUri("/api/login/oauth2/code/*"))
                        .successHandler(oauthSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            // Spring only reports these at DEBUG, and the redirect below hides
                            // them from the browser, so without this a failed sign-in leaves
                            // no trace anywhere.
                            log.warn("Google sign-in failed: {}", exception.getMessage(), exception);
                            response.sendRedirect(frontendUrl + "/login?error=oauth");
                        }))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204))
                        .deleteCookies("JSESSIONID"))
                .build();
    }
}
