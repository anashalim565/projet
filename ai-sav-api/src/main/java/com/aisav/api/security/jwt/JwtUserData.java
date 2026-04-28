package com.aisav.api.security.jwt;

import java.util.List;

public record JwtUserData(
        Long userId,
        String email,
        String tenantId,
        List<String> roles
) {
}