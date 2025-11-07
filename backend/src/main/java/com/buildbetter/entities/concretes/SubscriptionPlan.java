package com.buildbetter.entities.concretes;

import com.buildbetter.enums.PlanTier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private PlanTier tier;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price_monthly", nullable = false)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", nullable = false)
    private BigDecimal priceYearly;

    @Column(name = "stripe_price_id_monthly")
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_yearly")
    private String stripePriceIdYearly;

    @Column(name = "stripe_product_id")
    private String stripeProductId;

    // Quota limits
    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_ads")
    private Integer maxAds;

    @Column(name = "max_storage_mb")
    private Long maxStorageMb;

    @Column(name = "max_api_calls_per_hour")
    private Integer maxApiCallsPerHour;

    // Feature flags
    @Column(name = "custom_branding")
    private boolean customBranding = false;

    @Column(name = "priority_support")
    private boolean prioritySupport = false;

    @Column(name = "api_access")
    private boolean apiAccess = false;

    @Column(name = "advanced_analytics")
    private boolean advancedAnalytics = false;

    @Column(name = "white_label")
    private boolean whiteLabel = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "trial_days")
    private Integer trialDays = 14;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
