package com.buildbetter.core.interceptors;

import com.buildbetter.business.abstracts.OrganizationService;
import com.buildbetter.business.abstracts.UsageTrackingService;
import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.UsageMetricType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final UsageTrackingService usageTrackingService;
    private final OrganizationService organizationService;

    // In-memory rate limiting (for production, use Redis)
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    private static class RateLimitInfo {
        int count;
        LocalDateTime windowStart;

        RateLimitInfo() {
            this.count = 1;
            this.windowStart = LocalDateTime.now();
        }

        void increment() {
            this.count++;
        }

        boolean isExpired(int windowMinutes) {
            return ChronoUnit.MINUTES.between(windowStart, LocalDateTime.now()) >= windowMinutes;
        }

        void reset() {
            this.count = 1;
            this.windowStart = LocalDateTime.now();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip rate limiting for public endpoints
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true; // Let security handle unauthorized requests
        }

        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;

        if (organizationId != null) {
            // Track API call
            usageTrackingService.incrementUsage(organizationId, UsageMetricType.API_CALLS);

            // Check rate limit
            String rateLimitKey = organizationId + ":" + LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
            RateLimitInfo info = rateLimitMap.computeIfAbsent(rateLimitKey, k -> new RateLimitInfo());

            int windowMinutes = 60;
            int maxRequests = getMaxRequestsPerHour(organizationId);

            if (info.isExpired(windowMinutes)) {
                info.reset();
            } else {
                info.increment();
            }

            if (info.count > maxRequests) {
                response.setStatus(429); // Too Many Requests
                response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf(info.windowStart.plus(windowMinutes, ChronoUnit.MINUTES)));
                response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
                log.warn("Rate limit exceeded for organization: {}", organizationId);
                return false;
            }

            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(maxRequests - info.count));
        }

        return true;
    }

    private int getMaxRequestsPerHour(String organizationId) {
        try {
            Organization organization = organizationService.getOrganizationById(organizationId);
            if (organization.getSubscription() != null && organization.getSubscription().getPlan() != null) {
                Integer limit = organization.getSubscription().getPlan().getMaxApiCallsPerHour();
                return limit != null ? limit : 1000; // Default limit
            }
        } catch (Exception e) {
            log.error("Error fetching organization rate limit", e);
        }
        return 1000; // Default
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/ws/");
    }
}
