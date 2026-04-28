package com.aisav.api.dto;

public record AccessTokenResponse(
        String accessToken,
        UserDto user
) {}