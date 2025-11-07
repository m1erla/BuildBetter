package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.UsageTrackingService;
import com.buildbetter.dataAccess.abstracts.UsageTrackingRepository;
import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.entities.concretes.UsageTracking;
import com.buildbetter.enums.UsageMetricType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsageTrackingServiceImpl implements UsageTrackingService {

    private final UsageTrackingRepository usageTrackingRepository;

    @Override
    public void trackUsage(String organizationId, UsageMetricType metricType, long value) {
        Organization organization = new Organization();
        organization.setId(organizationId);

        UsageTracking tracking = UsageTracking.builder()
                .organization(organization)
                .metricType(metricType)
                .value(value)
                .timestamp(LocalDateTime.now())
                .build();

        usageTrackingRepository.save(tracking);
    }

    @Override
    public void trackUsage(String organizationId, String userId, UsageMetricType metricType, long value) {
        Organization organization = new Organization();
        organization.setId(organizationId);

        User user = new User();
        user.setId(userId);

        UsageTracking tracking = UsageTracking.builder()
                .organization(organization)
                .user(user)
                .metricType(metricType)
                .value(value)
                .timestamp(LocalDateTime.now())
                .build();

        usageTrackingRepository.save(tracking);
    }

    @Override
    public void incrementUsage(String organizationId, UsageMetricType metricType) {
        trackUsage(organizationId, metricType, 1L);
    }

    @Override
    public long getCurrentUsage(String organizationId, UsageMetricType metricType) {
        LocalDateTime startOfMonth = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
        return getUsageInPeriod(organizationId, metricType, startOfMonth, LocalDateTime.now());
    }

    @Override
    public long getUsageInPeriod(String organizationId, UsageMetricType metricType, LocalDateTime start, LocalDateTime end) {
        Long sum = usageTrackingRepository.sumUsageByOrganizationAndType(organizationId, metricType, start);
        return sum != null ? sum : 0L;
    }

    @Override
    public Map<UsageMetricType, Long> getAllUsageMetrics(String organizationId) {
        Map<UsageMetricType, Long> metrics = new HashMap<>();
        LocalDateTime startOfMonth = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);

        for (UsageMetricType metricType : UsageMetricType.values()) {
            long usage = getUsageInPeriod(organizationId, metricType, startOfMonth, LocalDateTime.now());
            metrics.put(metricType, usage);
        }

        return metrics;
    }

    @Override
    public boolean isWithinLimit(String organizationId, UsageMetricType metricType, long limit) {
        long currentUsage = getCurrentUsage(organizationId, metricType);
        return currentUsage < limit;
    }
}
