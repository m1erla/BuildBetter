package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.OrganizationService;
import com.buildbetter.business.abstracts.SubscriptionService;
import com.buildbetter.business.abstracts.UsageTrackingService;
import com.buildbetter.dataAccess.abstracts.*;
import com.buildbetter.enums.SubscriptionStatus;
import com.buildbetter.enums.UsageMetricType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/saas-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "SaaS Admin", description = "Advanced admin dashboard for SaaS metrics")
public class SaasAdminController {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final AdsRepository adsRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final UsageTrackingRepository usageTrackingRepository;

    @Data
    @Builder
    @AllArgsConstructor
    public static class SaasDashboardStats {
        private long totalOrganizations;
        private long activeOrganizations;
        private long totalUsers;
        private long totalExperts;
        private long totalAds;
        private long activeSubscriptions;
        private long trialingSubscriptions;
        private long canceledSubscriptions;
        private BigDecimal totalRevenue;
        private BigDecimal monthlyRecurringRevenue;
        private Map<String, Long> usageMetrics;
        private Map<String, Long> subscriptionsByPlan;
        private List<RecentActivity> recentActivities;
    }

    @Data
    @Builder
    public static class RecentActivity {
        private String type;
        private String description;
        private LocalDateTime timestamp;
        private String organizationId;
        private String userId;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get comprehensive SaaS dashboard statistics")
    public ResponseEntity<SaasDashboardStats> getDashboardStats() {
        long totalOrgs = organizationRepository.count();
        long activeOrgs = organizationRepository.countActiveOrganizations();
        long totalUsers = userRepository.count();
        long totalAds = adsRepository.count();
        long activeSubscriptions = subscriptionRepository.countActiveSubscriptions();

        // Get subscription counts by status
        long trialingSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.TRIALING).size();
        long canceledSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.CANCELED).size();

        // Calculate revenue
        BigDecimal totalRevenue = calculateTotalRevenue();
        BigDecimal mrr = calculateMRR();

        // Usage metrics
        Map<String, Long> usageMetrics = getUsageMetricsSummary();

        // Subscription distribution by plan
        Map<String, Long> subscriptionsByPlan = getSubscriptionsByPlan();

        SaasDashboardStats stats = SaasDashboardStats.builder()
                .totalOrganizations(totalOrgs)
                .activeOrganizations(activeOrgs)
                .totalUsers(totalUsers)
                .totalAds(totalAds)
                .activeSubscriptions(activeSubscriptions)
                .trialingSubscriptions(trialingSubscriptions)
                .canceledSubscriptions(canceledSubscriptions)
                .totalRevenue(totalRevenue)
                .monthlyRecurringRevenue(mrr)
                .usageMetrics(usageMetrics)
                .subscriptionsByPlan(subscriptionsByPlan)
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/revenue/metrics")
    @Operation(summary = "Get detailed revenue metrics")
    public ResponseEntity<Map<String, Object>> getRevenueMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRevenue", calculateTotalRevenue());
        metrics.put("monthlyRecurringRevenue", calculateMRR());
        metrics.put("averageRevenuePerUser", calculateARPU());
        metrics.put("churnRate", calculateChurnRate());
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/organizations/analytics")
    @Operation(summary = "Get organization analytics")
    public ResponseEntity<Map<String, Object>> getOrganizationAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalOrganizations", organizationRepository.count());
        analytics.put("activeOrganizations", organizationRepository.countActiveOrganizations());
        analytics.put("organizationsCreatedThisMonth", getOrganizationsCreatedThisMonth());
        analytics.put("averageUsersPerOrganization", calculateAverageUsersPerOrg());
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/subscriptions/analytics")
    @Operation(summary = "Get subscription analytics")
    public ResponseEntity<Map<String, Object>> getSubscriptionAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("activeSubscriptions", subscriptionRepository.countActiveSubscriptions());
        analytics.put("subscriptionsByStatus", getSubscriptionsByStatus());
        analytics.put("subscriptionsByPlan", getSubscriptionsByPlan());
        analytics.put("conversionRate", calculateTrialConversionRate());
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/usage/summary")
    @Operation(summary = "Get usage metrics summary")
    public ResponseEntity<Map<String, Long>> getUsageSummary() {
        return ResponseEntity.ok(getUsageMetricsSummary());
    }

    // Helper methods
    private BigDecimal calculateTotalRevenue() {
        // Sum all paid invoices
        return invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.isPaid())
                .map(invoice -> invoice.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateMRR() {
        // Calculate Monthly Recurring Revenue from active subscriptions
        return subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).stream()
                .map(sub -> sub.getAmount() != null ? sub.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateARPU() {
        long activeUsers = userRepository.count();
        if (activeUsers == 0) return BigDecimal.ZERO;
        return calculateMRR().divide(BigDecimal.valueOf(activeUsers), 2, BigDecimal.ROUND_HALF_UP);
    }

    private double calculateChurnRate() {
        long totalSubscriptions = subscriptionRepository.count();
        long canceledSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.CANCELED).size();
        if (totalSubscriptions == 0) return 0.0;
        return (double) canceledSubscriptions / totalSubscriptions * 100;
    }

    private double calculateTrialConversionRate() {
        long totalTrials = subscriptionRepository.findByStatus(SubscriptionStatus.TRIALING).size();
        long convertedTrials = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).size();
        if (totalTrials == 0) return 0.0;
        return (double) convertedTrials / (totalTrials + convertedTrials) * 100;
    }

    private Map<String, Long> getUsageMetricsSummary() {
        Map<String, Long> metrics = new HashMap<>();
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

        for (UsageMetricType metricType : UsageMetricType.values()) {
            List<UsageTracking> tracking = usageTrackingRepository.findByTimestampBetween(
                    startOfMonth, LocalDateTime.now());
            long sum = tracking.stream()
                    .filter(t -> t.getMetricType() == metricType)
                    .mapToLong(UsageTracking::getValue)
                    .sum();
            metrics.put(metricType.name(), sum);
        }
        return metrics;
    }

    private Map<String, Long> getSubscriptionsByPlan() {
        Map<String, Long> distribution = new HashMap<>();
        subscriptionRepository.findAll().forEach(sub -> {
            String planName = sub.getPlan() != null ? sub.getPlan().getName() : "No Plan";
            distribution.put(planName, distribution.getOrDefault(planName, 0L) + 1);
        });
        return distribution;
    }

    private Map<String, Long> getSubscriptionsByStatus() {
        Map<String, Long> distribution = new HashMap<>();
        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            long count = subscriptionRepository.findByStatus(status).size();
            distribution.put(status.name(), count);
        }
        return distribution;
    }

    private long getOrganizationsCreatedThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        return organizationRepository.findAll().stream()
                .filter(org -> org.getCreatedAt().isAfter(startOfMonth))
                .count();
    }

    private double calculateAverageUsersPerOrg() {
        long totalOrgs = organizationRepository.count();
        long totalUsers = userRepository.count();
        if (totalOrgs == 0) return 0.0;
        return (double) totalUsers / totalOrgs;
    }
}
