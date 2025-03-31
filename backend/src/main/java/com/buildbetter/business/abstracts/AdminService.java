package com.buildbetter.business.abstracts;

import com.buildbetter.business.responses.admin.DashboardStatsResponse;
import org.springframework.http.ResponseEntity;

public interface AdminService {
    DashboardStatsResponse getDashboardStats();

    Object getAllUsers();

    void deleteUser(String userId);

    Object getAllAds();

    void deleteAd(String adId);

    void updateAdStatus(String adId, boolean isActive);

    Object getAllStorages();

    void deleteStorage(String storageId);

    ResponseEntity<?> downloadStorage(String fileName);

    ResponseEntity<?> viewStorage(String fileName);
}