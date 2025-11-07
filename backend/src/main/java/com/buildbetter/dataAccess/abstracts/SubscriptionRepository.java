package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.Subscription;
import com.buildbetter.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByOrganizationId(String organizationId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    List<Subscription> findByStatus(SubscriptionStatus status);
    List<Subscription> findByPlanId(String planId);

    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd < :date AND s.status = :status")
    List<Subscription> findExpiringSoon(LocalDateTime date, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.trialEnd < :date AND s.status = 'TRIALING'")
    List<Subscription> findTrialsEndingSoon(LocalDateTime date);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIALING')")
    long countActiveSubscriptions();
}
