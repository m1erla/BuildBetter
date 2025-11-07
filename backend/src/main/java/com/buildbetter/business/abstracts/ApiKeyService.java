package com.buildbetter.business.abstracts;

import com.buildbetter.entities.concretes.ApiKey;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiKeyService {
    ApiKey createApiKey(String organizationId, String userId, String name, List<String> scopes, LocalDateTime expiresAt);
    ApiKey validateApiKey(String keyString);
    void revokeApiKey(String apiKeyId, String revokedByUserId);
    void trackApiKeyUsage(String apiKeyId);
    List<ApiKey> getOrganizationApiKeys(String organizationId);
    boolean hasScope(ApiKey apiKey, String scope);
}
