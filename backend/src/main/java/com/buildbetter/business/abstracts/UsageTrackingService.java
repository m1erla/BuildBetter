package com.buildbetter.business.abstracts;

import com.buildbetter.entities.concretes.UsageTracking;
import com.buildbetter.enums.UsageMetricType;

import java.time.LocalDateTime;
import java.util.Map;

public interface UsageTrackingService {
    void trackUsage(String organizationId, UsageMetricType metricType, long value);
    void trackUsage(String organizationId, String userId, UsageMetricType metricType, long value);
    void incrementUsage(String organizationId, UsageMetricType metricType);

    long getCurrentUsage(String organizationId, UsageMetricType metricType);
    long getUsageInPeriod(String organizationId, UsageMetricType metricType, LocalDateTime start, LocalDateTime end);
    Map<UsageMetricType, Long> getAllUsageMetrics(String organizationId);

    boolean isWithinLimit(String organizationId, UsageMetricType metricType, long limit);
}
