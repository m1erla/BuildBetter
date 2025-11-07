package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.FeatureFlagService;
import com.buildbetter.dataAccess.abstracts.FeatureFlagRepository;
import com.buildbetter.entities.concretes.FeatureFlag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagServiceImpl implements FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public boolean isEnabled(String flagKey) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .map(flag -> {
                    if (!flag.isEnabled()) {
                        return false;
                    }
                    // Check rollout percentage
                    return random.nextInt(100) < flag.getRolloutPercentage();
                })
                .orElse(false);
    }

    @Override
    public boolean isEnabledForOrganization(String flagKey, String organizationId) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .map(flag -> {
                    if (!flag.isEnabled()) {
                        return false;
                    }

                    // Check if organization is in target list
                    if (flag.getTargetOrganizations() != null) {
                        try {
                            List<String> targetOrgs = objectMapper.readValue(
                                    flag.getTargetOrganizations(),
                                    new TypeReference<List<String>>() {}
                            );
                            if (!targetOrgs.isEmpty() && !targetOrgs.contains(organizationId)) {
                                return false;
                            }
                        } catch (Exception e) {
                            log.error("Error parsing target organizations", e);
                        }
                    }

                    return random.nextInt(100) < flag.getRolloutPercentage();
                })
                .orElse(false);
    }

    @Override
    public boolean isEnabledForUser(String flagKey, String userId) {
        // Similar logic to organization but for users
        return isEnabled(flagKey);
    }

    @Override
    public FeatureFlag createFlag(String flagKey, String name, String description) {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey(flagKey)
                .name(name)
                .description(description)
                .isEnabled(false)
                .rolloutPercentage(0)
                .build();

        return featureFlagRepository.save(flag);
    }

    @Override
    public void enableFlag(String flagKey) {
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new RuntimeException("Feature flag not found"));
        flag.setEnabled(true);
        if (flag.getRolloutPercentage() == 0) {
            flag.setRolloutPercentage(100);
        }
        featureFlagRepository.save(flag);
    }

    @Override
    public void disableFlag(String flagKey) {
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new RuntimeException("Feature flag not found"));
        flag.setEnabled(false);
        featureFlagRepository.save(flag);
    }

    @Override
    public void setRolloutPercentage(String flagKey, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }

        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new RuntimeException("Feature flag not found"));
        flag.setRolloutPercentage(percentage);
        featureFlagRepository.save(flag);
    }
}
