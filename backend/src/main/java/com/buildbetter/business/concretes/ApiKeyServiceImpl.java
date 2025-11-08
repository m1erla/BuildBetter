package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.ApiKeyService;
import com.buildbetter.core.utilities.exceptions.BusinessException;
import com.buildbetter.dataAccess.abstracts.ApiKeyRepository;
import com.buildbetter.dataAccess.abstracts.OrganizationRepository;
import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.ApiKey;
import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "bb_live_";
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public ApiKey createApiKey(String organizationId, String userId, String name, List<String> scopes, LocalDateTime expiresAt) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("Organization not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Generate a secure random API key
        String keyString = generateSecureKey();
        String keyHash = hashKey(keyString);

        // Convert scopes to JSON string
        String scopesJson;
        try {
            scopesJson = objectMapper.writeValueAsString(scopes);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to process scopes");
        }

        ApiKey apiKey = ApiKey.builder()
                .organization(organization)
                .createdBy(user)
                .name(name)
                .keyHash(keyHash)
                .prefix(KEY_PREFIX)
                .scopes(scopesJson)
                .expiresAt(expiresAt)
                .isActive(true)
                .usageCount(0L)
                .build();

        return apiKeyRepository.save(apiKey);
    }

    @Override
    public ApiKey validateApiKey(String keyString) {
        if (keyString == null || !keyString.startsWith(KEY_PREFIX)) {
            throw new BusinessException("Invalid API key format");
        }

        String keyHash = hashKey(keyString);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash)
                .orElseThrow(() -> new BusinessException("Invalid API key"));

        if (!apiKey.isValid()) {
            throw new BusinessException("API key is not valid (expired, revoked, or inactive)");
        }

        return apiKey;
    }

    @Override
    @Transactional
    public void revokeApiKey(String apiKeyId, String revokedByUserId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("API key not found"));

        apiKey.setActive(false);
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKey.setRevokedByUserId(revokedByUserId);

        apiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public void trackApiKeyUsage(String apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("API key not found"));

        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKey.setUsageCount(apiKey.getUsageCount() + 1);

        apiKeyRepository.save(apiKey);
    }

    @Override
    public List<ApiKey> getOrganizationApiKeys(String organizationId) {
        return apiKeyRepository.findByOrganizationId(organizationId);
    }

    @Override
    public boolean hasScope(ApiKey apiKey, String scope) {
        if (apiKey.getScopes() == null) {
            return false;
        }

        try {
            List<String> scopes = objectMapper.readValue(
                    apiKey.getScopes(),
                    new TypeReference<List<String>>() {}
            );
            return scopes.contains(scope) || scopes.contains("*");
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Generates a secure random API key with the configured prefix
     */
    private String generateSecureKey() {
        byte[] randomBytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return KEY_PREFIX + randomPart;
    }

    /**
     * Hashes the API key using SHA-256
     */
    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("Failed to hash API key");
        }
    }
}
