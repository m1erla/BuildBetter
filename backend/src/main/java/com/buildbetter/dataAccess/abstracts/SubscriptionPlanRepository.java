package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.SubscriptionPlan;
import com.buildbetter.enums.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByName(String name);
    List<SubscriptionPlan> findByTier(PlanTier tier);
    List<SubscriptionPlan> findByIsActive(boolean isActive);
    Optional<SubscriptionPlan> findByStripeProductId(String stripeProductId);
    Optional<SubscriptionPlan> findByStripePriceIdMonthly(String stripePriceIdMonthly);
    Optional<SubscriptionPlan> findByStripePriceIdYearly(String stripePriceIdYearly);
}
