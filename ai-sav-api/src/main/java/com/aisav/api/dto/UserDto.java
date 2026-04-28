package com.aisav.api.dto;

public record UserDto(
        String id,
        String email,
        String tenantId,
        String role
) {}
