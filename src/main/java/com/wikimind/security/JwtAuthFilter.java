package com.wikimind.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads a Bearer token, and if it verifies, marks the request as authenticated. */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            try {
                Claims claims = jwtService.parse(header.substring(BEARER.length()));
                AuthUser principal = new AuthUser(claims.getSubject(), claims.get("orgId", String.class));
                var authority = new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, List.of(authority)));
            } catch (JwtException | IllegalArgumentException ex) {
                // invalid or expired token: leave the request unauthenticated so it gets a 401
            }
        }
        chain.doFilter(request, response);
    }
}
