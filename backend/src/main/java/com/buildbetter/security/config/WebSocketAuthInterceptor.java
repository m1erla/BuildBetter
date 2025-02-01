package com.renovatipoint.security.config;

import com.renovatipoint.security.jwt.JwtService;
import com.renovatipoint.security.token.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;
    private final UserDetailsService userService;
    private final TokenRepository tokenRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                String userEmail = jwtService.extractUsername(jwt);

                if (userEmail != null) {
                    UserDetails userDetails = userService.loadUserByUsername(userEmail);

                    // Verify token validity
                    var isTokenValid = tokenRepository.findByToken(jwt)
                            .map(t -> !t.isExpired() && !t.isRevoked())
                            .orElse(false);

                    if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
                        // Store user details in attributes for later use
                        attributes.put("username", userEmail);
                        attributes.put("USER_EMAIL", userEmail);
                        attributes.put("USER_DETAILS", userDetails);
                        return true;
                    }
                }
            }
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No action needed after handshake
    }
}