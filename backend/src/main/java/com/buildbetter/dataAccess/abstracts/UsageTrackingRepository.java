package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.UsageTracking;
import com.buildbetter.enums.UsageMetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, String> {
    List<UsageTracking> findByOrganizationId(String organizationId);
    List<UsageTracking> findByOrganizationIdAndMetricType(String organizationId, UsageMetricType metricType);
    List<UsageTracking> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(ut.value) FROM UsageTracking ut WHERE ut.organization.id = :organizationId AND ut.metricType = :metricType AND ut.timestamp >= :since")
    Long sumUsageByOrganizationAndType(String organizationId, UsageMetricType metricType, LocalDateTime since);

    @Query("SELECT ut FROM UsageTracking ut WHERE ut.organization.id = :organizationId AND ut.periodStart = :periodStart AND ut.periodEnd = :periodEnd")
    List<UsageTracking> findByOrganizationAndPeriod(String organizationId, LocalDateTime periodStart, LocalDateTime periodEnd);
}
