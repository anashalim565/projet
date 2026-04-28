package com.aisav.api.security.filter;

import com.aisav.api.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/auth/")
                || "/actuator/health".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        String header = req.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }

        try {
            String token = header.substring(BEARER_PREFIX.length());
            Claims claims = jwtService.validateAndExtract(token);

            List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(claims).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims.getSubject(),
                            null,
                            authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(req)
            );

            org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            chain.doFilter(req, res);

        } catch (ExpiredJwtException e) {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            writeUnauthorized(res, "{\"error\":\"Token expired\",\"code\":\"TOKEN_EXPIRED\"}");
        } catch (JwtException | IllegalArgumentException e) {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            writeUnauthorized(res, "{\"error\":\"Invalid token\",\"code\":\"INVALID_TOKEN\"}");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}