package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
    Optional<FeatureFlag> findByFlagKey(String flagKey);
    List<FeatureFlag> findByIsEnabled(boolean isEnabled);
}
