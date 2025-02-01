package com.renovatipoint.security.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renovatipoint.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);
        http
                .exceptionHandling(c -> c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .cors(Customizer.withDefaults())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/ws/**"))
                        .disable()
                )
                .authorizeHttpRequests(auth -> auth
                        // WebSocket endpoints
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/ws/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/ws")).permitAll()

                        // Auth endpoints
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/auth/**")).permitAll()

                        // Other public endpoints
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/payments/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/payments/webhook/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/invoices/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/transactions/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/requests/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/experts/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/categories/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/services/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/job_titles/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/ads/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/chat/**")).permitAll()

                        // Static resources
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/storage/**")).permitAll()

                        // Swagger/OpenAPI endpoints
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/v2/api-docs")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/configuration/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui.html")).permitAll()

                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout.logoutUrl("/api/v1/auth/logout")
                                .addLogoutHandler(logoutHandler)
                                .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }



    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/uploads/");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();

        // Allow your frontend origins
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://server.renovatipoint.com:32771",
                "http://server.renovatipoint.com:32771",
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:8080",
                "https://localhost:8443",
                "https://renovatipoint.com",
                "https://werkspot-development.netlify.app"
        ));

        // Allow WebSocket specific headers and methods
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration); // Specific WebSocket CORS config

        return source;
    }


}
//    @Bean
//    public ServletWebServerFactory servletContainer(){
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory(){
//            @Override
//            protected void postProcessContext(Context context) {
//                var securityConstraint = new SecurityConstraint();
//                securityConstraint.setUserConstraint("CONFIDENTIAL");
//                var collection = new SecurityCollection();
//                collection.addPattern("/*");
//                securityConstraint.addCollection(collection);
//                context.addConstraint(securityConstraint);
//            }
//        };
//        tomcat.addAdditionalTomcatConnectors(getHttpConnector());
//        return tomcat;
//    }
//
//    private Connector getHttpConnector(){
//        var connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
//        connector.setScheme("http");
//        connector.setPort(8080);
//        connector.setSecure(false);
//        connector.setRedirectPort(8443);
//        return connector;
//    }

//    private Connector getHttpConnector(){
//        var connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
//        connector.setScheme("http");
//        connector.setPort(8081);
//        connector.setSecure(true);
//        connector.setRedirectPort(8443);
//        return connector;
//    }


//    @Bean
//    public SecurityFilterChain securityFilterChainGlobalAPI(HttpSecurity http) throws Exception{
//        sharedSecurityConfiguration(http);
//        http.securityMatcher("/api/v1/users", "/api/v1/admin").authorizeHttpRequests(auth ->{
//            auth.anyRequest().authenticated();
//        }).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChainGlobalAdminAPI(HttpSecurity http) throws Exception{
//        sharedSecurityConfiguration(http);
//        http.securityMatcher("/api/v1/admin/**").authorizeHttpRequests(auth ->{
//            auth.anyRequest()
//                    .hasRole("ADMIN");
//        }).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChainGlobalUserProfileAPI(HttpSecurity http) throws Exception{
//        sharedSecurityConfiguration(http);
//        http.securityMatcher("/api/v1/users/profile").authorizeHttpRequests(auth ->{
//            auth.anyRequest().hasRole("USER");
//
//        }).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }





