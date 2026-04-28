

package com.aisav.api.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TENANT = "tenant";

    private final JwtProperties jwtProperties;
    private final JwtKeyProvider jwtKeyProvider;

    public JwtService(JwtProperties jwtProperties, JwtKeyProvider jwtKeyProvider) {
        this.jwtProperties = jwtProperties;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    public String generateAccessToken(JwtUserData userData) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(String.valueOf(userData.userId()))
                .claim(CLAIM_EMAIL, userData.email())
                .claim(CLAIM_ROLES, userData.roles())
                .claim(CLAIM_TENANT, userData.tenantId())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessExpiryMs()))
                .signWith(jwtKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateAndExtract(String token) throws JwtException, ExpiredJwtException {
        return Jwts.parser()
                .verifyWith(jwtKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        Claims claims = validateAndExtract(token);
        return Long.valueOf(claims.getSubject());
    }

    public String extractEmail(String token) {
        Claims claims = validateAndExtract(token);
        return claims.get(CLAIM_EMAIL, String.class);
    }

    public String extractTenant(String token) {
        Claims claims = validateAndExtract(token);
        return claims.get(CLAIM_TENANT, String.class);
    }

    public List<String> extractRoles(String token) {
        Claims claims = validateAndExtract(token);
        return extractRoles(claims);
    }

    public List<String> extractRoles(Claims claims) {
        Object rolesObject = claims.get(CLAIM_ROLES);

        if (rolesObject == null) {
            return List.of();
        }

        if (rolesObject instanceof List<?> rawList) {
            return rawList.stream()
                    .map(String::valueOf)
                    .toList();
        }

        throw new IllegalStateException("Invalid roles claim format");
    }

    public boolean isExpired(String token) {
        try {
            Claims claims = validateAndExtract(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}