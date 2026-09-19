package com.invoisuite.server.service;

import com.invoisuite.server.common.BadRequestException;
import com.invoisuite.server.domain.AuthProvider;
import com.invoisuite.server.domain.User;
import com.invoisuite.server.dto.auth.AuthResponse;
import com.invoisuite.server.dto.auth.GoogleAuthRequest;
import com.invoisuite.server.dto.auth.LoginRequest;
import com.invoisuite.server.dto.auth.RegisterRequest;
import com.invoisuite.server.dto.auth.UserResponse;
import com.invoisuite.server.repository.UserRepository;
import com.invoisuite.server.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .authProvider(AuthProvider.LOCAL)
                .build();
        user = userRepository.save(user);

        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        return issueToken(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleTokenVerifierService.GoogleUserInfo googleUser = googleTokenVerifierService.verify(request.idToken());

        User user = userRepository.findByGoogleSub(googleUser.sub())
                .or(() -> userRepository.findByEmail(googleUser.email()))
                .map(existing -> linkGoogleAccount(existing, googleUser))
                .orElseGet(() -> createGoogleUser(googleUser));

        return issueToken(user);
    }

    private User linkGoogleAccount(User user, GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleUser.sub());
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
            userRepository.save(user);
        }
        return user;
    }

    private User createGoogleUser(GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        User user = User.builder()
                .email(googleUser.email().toLowerCase())
                .fullName(googleUser.fullName())
                .authProvider(AuthProvider.GOOGLE)
                .googleSub(googleUser.sub())
                .build();
        return userRepository.save(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
