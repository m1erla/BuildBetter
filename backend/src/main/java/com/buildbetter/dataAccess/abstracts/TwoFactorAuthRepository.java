package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.TwoFactorAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, String> {
    Optional<TwoFactorAuth> findByUserId(String userId);
    Optional<TwoFactorAuth> findByUserIdAndIsEnabled(String userId, boolean isEnabled);
}
