package com.aisav.api.mapper;

import com.aisav.api.dto.AccessTokenResponse;
import com.aisav.api.dto.LoginRequest;
import com.aisav.api.dto.UserDto;
import com.aisav.api.security.jwt.JwtUserData;
import com.aisav.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public String toEmail(LoginRequest request) {
        return request.email();
    }

    public String toRawPassword(LoginRequest request) {
        return request.password();
    }

    public JwtUserData toJwtUserData(User user) {
        return new JwtUserData(
                user.getId(),
                user.getEmail(),
                user.getTenantId(),
                user.getRoles().stream().toList()
        );
    }

    public UserDto toUserDto(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getTenantId(),
                user.getRoles().isEmpty() ? "AGENT" : user.getRoles().iterator().next()
        );
    }

    public AccessTokenResponse toAccessTokenResponse(String accessToken, User user) {
        return new AccessTokenResponse(accessToken, toUserDto(user));
    }
}