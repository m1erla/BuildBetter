package com.buildbetter.business.abstracts;

import com.buildbetter.business.responses.admin.DashboardStatsResponse;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

public interface AdminService {
    DashboardStatsResponse getDashboardStats();

    Object getAllUsers();

    void deleteUser(String userId);

    // Bu metot ExpertService arayüzüne eklenmeli
    @Transactional
    ResponseEntity<?> deleteExpertAndAssociatedData(String expertId) throws StripeException;

    // Bu metot UserService arayüzüne eklenmeli
    @Transactional
    ResponseEntity<?> deleteUserAndAssociatedData(String id);

    Object getAllAds();

    void deleteAd(String adId);

    void updateAdStatus(String adId, boolean isActive);

    Object getAllStorages();

    void deleteStorage(String storageId);

    ResponseEntity<?> downloadStorage(String fileName);

    ResponseEntity<?> viewStorage(String fileName);
}