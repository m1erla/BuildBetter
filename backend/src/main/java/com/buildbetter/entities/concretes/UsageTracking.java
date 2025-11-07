package com.buildbetter.entities.concretes;

import com.buildbetter.enums.UsageMetricType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usage_tracking", indexes = {
    @Index(name = "idx_usage_organization", columnList = "organization_id"),
    @Index(name = "idx_usage_timestamp", columnList = "timestamp"),
    @Index(name = "idx_usage_metric_type", columnList = "metric_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private UsageMetricType metricType;

    @Column(name = "value", nullable = false)
    private Long value;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional context

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
