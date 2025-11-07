package com.buildbetter.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags", indexes = {
    @Index(name = "idx_feature_flag_key", columnList = "flag_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "flag_key", nullable = false, unique = true)
    private String flagKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_enabled")
    private boolean isEnabled = false;

    @Column(name = "rollout_percentage")
    private Integer rolloutPercentage = 0; // 0-100 for gradual rollout

    @Column(name = "target_plan_tiers", columnDefinition = "TEXT")
    private String targetPlanTiers; // JSON array of plan tiers

    @Column(name = "target_organizations", columnDefinition = "TEXT")
    private String targetOrganizations; // JSON array of organization IDs

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

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
