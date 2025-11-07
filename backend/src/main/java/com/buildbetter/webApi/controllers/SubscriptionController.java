package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.SubscriptionService;
import com.buildbetter.entities.concretes.Subscription;
import com.buildbetter.entities.concretes.SubscriptionPlan;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.BillingInterval;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription and billing management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    @Operation(summary = "Get all active subscription plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(subscriptionService.getAllActivePlans());
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Get subscription plan by ID")
    public ResponseEntity<SubscriptionPlan> getPlan(@PathVariable String id) {
        return ResponseEntity.ok(subscriptionService.getPlanById(id));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create a new subscription plan (Admin only)")
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan created = subscriptionService.createPlan(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping
    @Operation(summary = "Create subscription for organization")
    public ResponseEntity<Subscription> createSubscription(
            @RequestParam String planId,
            @RequestParam BillingInterval interval,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();
        Subscription subscription = subscriptionService.createSubscription(organizationId, planId, interval);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current user's organization subscription")
    public ResponseEntity<Subscription> getCurrentSubscription(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();
        Subscription subscription = subscriptionService.getSubscriptionByOrganizationId(organizationId);
        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/upgrade")
    @Operation(summary = "Upgrade subscription to a higher plan")
    public ResponseEntity<Subscription> upgradeSubscription(
            @RequestParam String newPlanId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();
        Subscription upgraded = subscriptionService.upgradeSubscription(organizationId, newPlanId);
        return ResponseEntity.ok(upgraded);
    }

    @PostMapping("/downgrade")
    @Operation(summary = "Downgrade subscription to a lower plan")
    public ResponseEntity<Subscription> downgradeSubscription(
            @RequestParam String newPlanId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();
        Subscription downgraded = subscriptionService.downgradeSubscription(organizationId, newPlanId);
        return ResponseEntity.ok(downgraded);
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel subscription")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable String subscriptionId,
            @RequestParam(defaultValue = "false") boolean immediate) {
        subscriptionService.cancelSubscription(subscriptionId, immediate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriptionId}/reactivate")
    @Operation(summary = "Reactivate cancelled subscription")
    public ResponseEntity<Void> reactivateSubscription(@PathVariable String subscriptionId) {
        subscriptionService.reactivateSubscription(subscriptionId);
        return ResponseEntity.ok().build();
    }
}
