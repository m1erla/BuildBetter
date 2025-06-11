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
import io.github.cdimascio.dotenv.Dotenv;

@RestControllerAdvice
@OpenAPIDefinition
@SpringBootApplication
@PropertySource("classpath:application.yml")
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class BackendApplication {

    public static void main(String[] args) {
        // Only load .env in development environments
        try {
            // Check if we're in a production environment
            String profile = System.getenv("SPRING_PROFILES_ACTIVE");
            boolean isProduction = profile != null && profile.contains("prod");

            if (!isProduction) {
                // Only try to load .env in non-production environments
                Dotenv dotenv = Dotenv.configure()
                        .directory(".")
                        .filename(".env")
                        .ignoreIfMissing()
                        .load();

                dotenv.entries().forEach(entry -> {
                    if (System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
                System.out.println("Loaded .env file for development");
            } else {
                System.out.println("Production environment detected, using system environment variables");
            }
        } catch (Exception e) {
            System.out.println("Could not load .env file (this is normal in production): " + e.getMessage());
        }

        SpringApplication.run(BackendApplication.class, args);
    }
}
