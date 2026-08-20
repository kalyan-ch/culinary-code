package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.RecipeUserRepository;
import com.wb.culinaryCode.model.auth.AuthDTOs.LoginRequest;
import com.wb.culinaryCode.model.auth.AuthDTOs.RegisterRequest;
import com.wb.culinaryCode.model.auth.AuthDTOs.UserDTO;
import com.wb.culinaryCode.model.recipe.RecipeUser;
import com.wb.culinaryCode.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RecipeUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public UserDTO register(RegisterRequest request, HttpServletRequest req, HttpServletResponse res) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered");
        }

        users.save(RecipeUser.builder()
                .email(request.email().toLowerCase())
                .displayName(request.displayName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());

        // Sign the new account straight in; making someone log in immediately after
        // registering is friction with no security benefit.
        return login(new LoginRequest(request.email(), request.password()), req, res);
    }

    /**
     * Authenticates, then persists the resulting context into the HTTP session. Spring only
     * does this automatically for its own login filters — a controller-driven login has to
     * save the context itself, or the user stays authenticated for exactly one request.
     */
    public UserDTO login(LoginRequest request, HttpServletRequest req, HttpServletResponse res) {
        // A Google-created account has no hash, so BCrypt would just report "incorrect
        // password" and leave the user guessing at a password they never set.
        users.findByEmailIgnoreCase(request.email())
                .filter(user -> user.getPasswordHash() == null)
                .ifPresent(user -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "This account uses Google sign-in");
                });

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, req, res);

        return UserDTO.of((AuthUser) authentication.getPrincipal());
    }
}
