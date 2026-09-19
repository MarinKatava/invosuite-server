package com.invoisuite.server.web;

import com.invoisuite.server.dto.auth.AuthResponse;
import com.invoisuite.server.dto.auth.GoogleAuthRequest;
import com.invoisuite.server.dto.auth.LoginRequest;
import com.invoisuite.server.dto.auth.RegisterRequest;
import com.invoisuite.server.dto.auth.UserResponse;
import com.invoisuite.server.repository.UserRepository;
import com.invoisuite.server.security.CurrentUser;
import com.invoisuite.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        var user = userRepository.findById(CurrentUser.id()).orElseThrow();
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
