package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.AdminService;
import com.buildbetter.business.responses.admin.DashboardStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ads")
    public ResponseEntity<?> getAllAds() {
        return ResponseEntity.ok(adminService.getAllAds());
    }

    @DeleteMapping("/ads/{adId}")
    public ResponseEntity<?> deleteAd(@PathVariable String adId) {
        adminService.deleteAd(adId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/ads/{adId}/status")
    public ResponseEntity<?> updateAdStatus(
            @PathVariable String adId,
            @RequestBody UpdateAdStatusRequest request) {
        adminService.updateAdStatus(adId, request.isActive());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/storages")
    public ResponseEntity<?> getAllStorages() {
        return ResponseEntity.ok(adminService.getAllStorages());
    }

    @DeleteMapping("/storages/{storageId}")
    public ResponseEntity<?> deleteStorage(@PathVariable String storageId) {
        adminService.deleteStorage(storageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/storages/download/{fileName}")
    public ResponseEntity<?> downloadStorage(@PathVariable String fileName) {
        return adminService.downloadStorage(fileName);
    }

    @GetMapping("/storages/view/{fileName}")
    public ResponseEntity<?> viewStorage(@PathVariable String fileName) {
        return adminService.viewStorage(fileName);
    }

    // Yardımcı sınıf
    public static class UpdateAdStatusRequest {
        private boolean isActive;

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }
    }
}