package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.SubscriptionService;
import com.buildbetter.core.utilities.exceptions.BusinessException;
import com.buildbetter.dataAccess.abstracts.OrganizationRepository;
import com.buildbetter.dataAccess.abstracts.SubscriptionPlanRepository;
import com.buildbetter.dataAccess.abstracts.SubscriptionRepository;
import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.Subscription;
import com.buildbetter.entities.concretes.SubscriptionPlan;
import com.buildbetter.enums.BillingInterval;
import com.buildbetter.enums.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        if (plan.getId() == null) {
            plan.setId(UUID.randomUUID().toString());
        }
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        return subscriptionPlanRepository.save(plan);
    }

    @Override
    public SubscriptionPlan getPlanById(String id) {
        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Subscription plan not found with id: " + id));
    }

    @Override
    public List<SubscriptionPlan> getAllActivePlans() {
        return subscriptionPlanRepository.findByIsActive(true);
    }

    @Override
    @Transactional
    public SubscriptionPlan updatePlan(String id, SubscriptionPlan plan) {
        SubscriptionPlan existingPlan = getPlanById(id);

        existingPlan.setName(plan.getName());
        existingPlan.setDescription(plan.getDescription());
        existingPlan.setTier(plan.getTier());
        existingPlan.setPriceMonthly(plan.getPriceMonthly());
        existingPlan.setPriceYearly(plan.getPriceYearly());
        existingPlan.setFeatures(plan.getFeatures());
        existingPlan.setQuotas(plan.getQuotas());
        existingPlan.setUpdatedAt(LocalDateTime.now());

        return subscriptionPlanRepository.save(existingPlan);
    }

    @Override
    @Transactional
    public Subscription createSubscription(String organizationId, String planId, BillingInterval interval) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("Organization not found with id: " + organizationId));

        SubscriptionPlan plan = getPlanById(planId);

        // Check if organization already has an active subscription
        subscriptionRepository.findByOrganizationId(organizationId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == SubscriptionStatus.ACTIVE ||
                        existing.getStatus() == SubscriptionStatus.TRIALING) {
                        throw new BusinessException("Organization already has an active subscription");
                    }
                });

        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID().toString())
                .organization(organization)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval(interval)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(calculatePeriodEnd(LocalDateTime.now(), interval))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Override
    public Subscription getSubscriptionByOrganizationId(String organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new BusinessException("No subscription found for organization: " + organizationId));
    }

    @Override
    @Transactional
    public Subscription upgradeSubscription(String organizationId, String newPlanId) {
        Subscription subscription = getSubscriptionByOrganizationId(organizationId);
        SubscriptionPlan newPlan = getPlanById(newPlanId);

        subscription.setPlan(newPlan);
        subscription.setUpdatedAt(LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public Subscription downgradeSubscription(String organizationId, String newPlanId) {
        Subscription subscription = getSubscriptionByOrganizationId(organizationId);
        SubscriptionPlan newPlan = getPlanById(newPlanId);

        subscription.setPlan(newPlan);
        subscription.setUpdatedAt(LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void cancelSubscription(String subscriptionId, boolean immediate) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException("Subscription not found with id: " + subscriptionId));

        if (immediate) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(LocalDateTime.now());
        } else {
            subscription.setStatus(SubscriptionStatus.PENDING_CANCELLATION);
            subscription.setCancelAt(subscription.getCurrentPeriodEnd());
        }

        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void reactivateSubscription(String subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException("Subscription not found with id: " + subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.CANCELED &&
            subscription.getStatus() != SubscriptionStatus.PENDING_CANCELLATION) {
            throw new BusinessException("Only canceled or pending cancellation subscriptions can be reactivated");
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCanceledAt(null);
        subscription.setCancelAt(null);
        subscription.setUpdatedAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public Subscription syncWithStripe(String stripeSubscriptionId) {
        // TODO: Implement Stripe integration
        throw new UnsupportedOperationException("Stripe sync not yet implemented");
    }

    @Override
    public void handleStripeWebhook(String payload, String signature) {
        // TODO: Implement Stripe webhook handling
        throw new UnsupportedOperationException("Stripe webhook handling not yet implemented");
    }

    @Override
    public boolean hasFeatureAccess(String organizationId, String featureName) {
        try {
            Subscription subscription = getSubscriptionByOrganizationId(organizationId);

            if (subscription.getStatus() != SubscriptionStatus.ACTIVE &&
                subscription.getStatus() != SubscriptionStatus.TRIALING) {
                return false;
            }

            // TODO: Implement proper feature checking based on plan features
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    @Override
    public boolean isWithinQuota(String organizationId, String quotaType, long currentUsage) {
        try {
            Subscription subscription = getSubscriptionByOrganizationId(organizationId);

            if (subscription.getStatus() != SubscriptionStatus.ACTIVE &&
                subscription.getStatus() != SubscriptionStatus.TRIALING) {
                return false;
            }

            // TODO: Implement proper quota checking based on plan quotas
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private LocalDateTime calculatePeriodEnd(LocalDateTime start, BillingInterval interval) {
        return switch (interval) {
            case MONTHLY -> start.plusMonths(1);
            case YEARLY -> start.plusYears(1);
        };
    }
}
