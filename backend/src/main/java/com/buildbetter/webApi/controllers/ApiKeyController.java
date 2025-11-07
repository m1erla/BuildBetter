package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.ApiKeyService;
import com.buildbetter.entities.concretes.ApiKey;
import com.buildbetter.entities.concretes.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "API key management for programmatic access")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Data
    public static class CreateApiKeyRequest {
        private String name;
        private List<String> scopes;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class ApiKeyResponse {
        private String id;
        private String name;
        private String key; // Only returned once during creation
        private String prefix;
        private List<String> scopes;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private boolean isActive;
    }

    @PostMapping
    @Operation(summary = "Create a new API key")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();

        ApiKey apiKey = apiKeyService.createApiKey(
                organizationId,
                user.getId(),
                request.getName(),
                request.getScopes(),
                request.getExpiresAt()
        );

        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setName(apiKey.getName());
        response.setPrefix(apiKey.getPrefix());
        // Note: In real implementation, return the actual key only once
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setActive(apiKey.isActive());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all API keys for the organization")
    public ResponseEntity<List<ApiKey>> listApiKeys(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();
        List<ApiKey> apiKeys = apiKeyService.getOrganizationApiKeys(organizationId);
        return ResponseEntity.ok(apiKeys);
    }

    @DeleteMapping("/{apiKeyId}")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable String apiKeyId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        apiKeyService.revokeApiKey(apiKeyId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
