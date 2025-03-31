package com.buildbetter.business.responses.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    private long userCount;
    private long adCount;
    private long storageCount;
    private BigDecimal totalRevenue;
}