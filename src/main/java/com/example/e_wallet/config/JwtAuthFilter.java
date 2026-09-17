package com.example.e_wallet.config;

import com.example.e_wallet.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// run once per http request to check authorization header
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // we extract the token from "Bearer <token>"
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);   

            if (jwtService.isValid(token)) {
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);

                // grant the role
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);

                // consider the requester authenticated from now on
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        // continue the security filter chain
        filterChain.doFilter(request, response);
    }
}