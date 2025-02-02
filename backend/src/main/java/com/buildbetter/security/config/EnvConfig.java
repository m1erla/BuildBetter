package com.buildbetter.security.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    public EnvConfig() {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")  // Look for .env in project root
                .ignoreIfMissing()
                .load();

        // Set environment variables from .env
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }
}