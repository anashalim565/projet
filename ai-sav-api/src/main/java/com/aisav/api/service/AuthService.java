package com.aisav.api.service;

import com.aisav.api.entity.RefreshToken;
import com.aisav.api.repository.RefreshTokenRepository;
import com.aisav.api.common.exception.InvalidTokenException;
import com.aisav.api.common.exception.UnauthorizedException;
import com.aisav.api.security.jwt.JwtProperties;
import com.aisav.api.entity.User;
import com.aisav.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return user;
    }

    public String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpiryMs()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional(readOnly = true)
    public User validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token is missing");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired"));

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found for refresh token"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        return user;
    }

    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
}