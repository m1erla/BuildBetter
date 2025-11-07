package com.buildbetter.business.abstracts;

import com.buildbetter.entities.concretes.FeatureFlag;

public interface FeatureFlagService {
    boolean isEnabled(String flagKey);
    boolean isEnabledForOrganization(String flagKey, String organizationId);
    boolean isEnabledForUser(String flagKey, String userId);

    FeatureFlag createFlag(String flagKey, String name, String description);
    void enableFlag(String flagKey);
    void disableFlag(String flagKey);
    void setRolloutPercentage(String flagKey, int percentage);
}
