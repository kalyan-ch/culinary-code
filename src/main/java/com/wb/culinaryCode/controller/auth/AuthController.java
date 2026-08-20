package com.wb.culinaryCode.controller.auth;

import com.wb.culinaryCode.model.auth.AuthDTOs.LoginRequest;
import com.wb.culinaryCode.model.auth.AuthDTOs.RegisterRequest;
import com.wb.culinaryCode.model.auth.AuthDTOs.UserDTO;
import com.wb.culinaryCode.security.AuthUser;
import com.wb.culinaryCode.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Logout is handled by the filter chain — see {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request,
                                            HttpServletRequest req, HttpServletResponse res) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, req, res));
    }

    @PostMapping("/login")
    public UserDTO login(@Valid @RequestBody LoginRequest request,
                         HttpServletRequest req, HttpServletResponse res) {
        return authService.login(request, req, res);
    }

    @GetMapping("/me")
    public UserDTO me(@AuthenticationPrincipal AuthUser user) {
        return UserDTO.of(user);
    }
}
