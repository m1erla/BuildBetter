package com.buildbetter.business.abstracts;

import com.buildbetter.entities.concretes.Subscription;
import com.buildbetter.entities.concretes.SubscriptionPlan;
import com.buildbetter.enums.BillingInterval;

import java.util.List;

public interface SubscriptionService {
    // Plan management
    SubscriptionPlan createPlan(SubscriptionPlan plan);
    SubscriptionPlan getPlanById(String id);
    List<SubscriptionPlan> getAllActivePlans();
    SubscriptionPlan updatePlan(String id, SubscriptionPlan plan);

    // Subscription management
    Subscription createSubscription(String organizationId, String planId, BillingInterval interval);
    Subscription getSubscriptionByOrganizationId(String organizationId);
    Subscription upgradeSubscription(String organizationId, String newPlanId);
    Subscription downgradeSubscription(String organizationId, String newPlanId);
    void cancelSubscription(String subscriptionId, boolean immediate);
    void reactivateSubscription(String subscriptionId);

    // Stripe integration
    Subscription syncWithStripe(String stripeSubscriptionId);
    void handleStripeWebhook(String payload, String signature);

    // Usage checks
    boolean hasFeatureAccess(String organizationId, String featureName);
    boolean isWithinQuota(String organizationId, String quotaType, long currentUsage);
}
