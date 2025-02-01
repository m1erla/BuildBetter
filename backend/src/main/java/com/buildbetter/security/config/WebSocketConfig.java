package com.renovatipoint.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renovatipoint.security.jwt.JwtAuthenticationFilter;
import com.renovatipoint.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:[*]",
                        "https://server.renovatipoint.com:32771",
                        "http://server.renovatipoint.com:32771",
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://localhost:8443",
                        "https://renovatipoint.com",
                        "https://werkspot-development.netlify.app")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            String token = servletRequest.getServletRequest().getParameter("token");
                            if (token != null && token.startsWith("Bearer ")) {
                                token = token.substring(7);
                            }
                            if (token != null) {
                                String username = jwtService.extractUsername(token);
                                if (username != null) {
                                    attributes.put("username", username);
                                    return true;
                                }
                            }
                        }
                        return true; // Allow connection even without token, security will be handled at message level
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                })
                .withSockJS();
    }

@Override
public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setObjectMapper(objectMapper);
    messageConverters.add(converter);
    return false;
}


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    String token = authorization != null && !authorization.isEmpty() ? authorization.get(0) : null;

                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        try {
                            String username = jwtService.extractUsername(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            if (jwtService.isTokenValid(token, userDetails)) {
                                Authentication auth = new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        userDetails.getAuthorities()
                                );
                                accessor.setUser(auth);
                            }
                        } catch (Exception e) {
                            log.error("Error authenticating WebSocket connection", e);
                        }
                    }
                }
                return message;
            }
        });
    }
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(8192)
                .setSendBufferSizeLimit(8192)
                .setSendTimeLimit(10000);
    }

}
