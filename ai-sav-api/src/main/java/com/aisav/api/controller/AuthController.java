package com.aisav.api.controller;

import com.aisav.api.dto.AccessTokenResponse;
import com.aisav.api.dto.LoginRequest;
import com.aisav.api.mapper.AuthMapper;
import com.aisav.api.service.AuthService;
import com.aisav.api.security.jwt.JwtService;
import com.aisav.api.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          AuthMapper authMapper) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        User user = authService.authenticate(
                authMapper.toEmail(request),
                authMapper.toRawPassword(request)
        );

        String accessToken = jwtService.generateAccessToken(authMapper.toJwtUserData(user));
        String refreshToken = authService.generateRefreshToken(user);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(refreshExpiryMs / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(authMapper.toAccessTokenResponse(accessToken, user)); // ← user ajouté
    }


    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        User user = authService.validateRefreshToken(refreshToken);
        String accessToken = jwtService.generateAccessToken(authMapper.toJwtUserData(user));

        return ResponseEntity.ok(authMapper.toAccessTokenResponse(accessToken, user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        authService.revokeRefreshToken(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok().build();
    }
}