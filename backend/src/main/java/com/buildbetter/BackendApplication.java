package com.buildbetter;

import com.buildbetter.business.requests.RegisterRequest;
import com.buildbetter.security.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@OpenAPIDefinition
@SpringBootApplication
@PropertySource("classpath:application.yml")
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
    @Bean
    public CommandLineRunner commandLineRunner(
            AuthenticationService service
    ) {
        return args -> {
            var admin = RegisterRequest.builder()
                    .build();
            var manager = RegisterRequest.builder()
                    .build();
        };
    }
}
