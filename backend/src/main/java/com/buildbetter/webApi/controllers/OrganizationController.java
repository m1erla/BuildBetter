package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.OrganizationService;
import com.buildbetter.entities.concretes.Organization;
import com.buildbetter.entities.concretes.OrganizationMember;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.OrganizationRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization and workspace management")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<Organization> createOrganization(
            @RequestBody Organization organization,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Organization created = organizationService.createOrganization(organization, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<Organization> getOrganization(@PathVariable String id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get organization by slug")
    public ResponseEntity<Organization> getOrganizationBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(organizationService.getOrganizationBySlug(slug));
    }

    @GetMapping
    @Operation(summary = "Get all organizations")
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable String id,
            @RequestBody Organization organization) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, organization));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete organization")
    public ResponseEntity<Void> deleteOrganization(@PathVariable String id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get organization members")
    public ResponseEntity<List<OrganizationMember>> getMembers(@PathVariable String id) {
        return ResponseEntity.ok(organizationService.getOrganizationMembers(id));
    }

    @PostMapping("/{organizationId}/members")
    @Operation(summary = "Add member to organization")
    public ResponseEntity<OrganizationMember> addMember(
            @PathVariable String organizationId,
            @RequestParam String userId,
            @RequestParam OrganizationRole role) {
        OrganizationMember member = organizationService.addMember(organizationId, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @DeleteMapping("/{organizationId}/members/{userId}")
    @Operation(summary = "Remove member from organization")
    public ResponseEntity<Void> removeMember(
            @PathVariable String organizationId,
            @PathVariable String userId) {
        organizationService.removeMember(organizationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{organizationId}/members/{userId}/role")
    @Operation(summary = "Update member role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable String organizationId,
            @PathVariable String userId,
            @RequestParam OrganizationRole role) {
        organizationService.updateMemberRole(organizationId, userId, role);
        return ResponseEntity.ok().build();
    }
}
