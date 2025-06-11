package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.AdminService;
import com.buildbetter.business.abstracts.AdsService;
import com.buildbetter.business.abstracts.UserService;
import com.buildbetter.business.responses.GetAllAdsResponse;
import com.buildbetter.business.responses.GetAllImagesResponse;
import com.buildbetter.business.responses.GetUsersResponse;
import com.buildbetter.business.responses.admin.DashboardStatsResponse;
import com.buildbetter.core.utilities.exceptions.BusinessException;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.*;
import com.buildbetter.entities.concretes.Ads;
import com.buildbetter.entities.concretes.Expert;
import com.buildbetter.entities.concretes.Storage;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.security.token.Token;
import com.buildbetter.security.token.TokenRepository;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminManager implements AdminService {

    // Removed UPLOAD_DIR, let StorageManager handle paths
    private static final Logger logger = LoggerFactory.getLogger(AdminManager.class); // Logger

    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final TokenRepository tokenRepository;
    private final StripeManager stripeManager;
    private final AdsRepository adsRepository;
    private final StorageRepository storageRepository;
    private final InvoiceRepository invoiceRepository;
    private final ModelMapperService modelMapperService;
    private final StorageManager storageManager; // Use StorageService interface if preferred
    private final UserService userService; // Inject UserService
    private final AdsService adsService;   // Inject AdsService

    @Autowired
    public AdminManager(
            UserRepository userRepository,
            ExpertRepository expertRepository, TokenRepository tokenRepository, StripeManager stripeManager, AdsRepository adsRepository,
            StorageRepository storageRepository,
            InvoiceRepository invoiceRepository,
            ModelMapperService modelMapperService,
            StorageManager storageManager,
            UserService userService, // Inject
            AdsService adsService    // Inject
    ) {
        this.userRepository = userRepository;
        this.expertRepository = expertRepository;
        this.tokenRepository = tokenRepository;
        this.stripeManager = stripeManager;
        this.adsRepository = adsRepository;
        this.storageRepository = storageRepository;
        this.invoiceRepository = invoiceRepository;
        this.modelMapperService = modelMapperService;
        this.storageManager = storageManager;
        this.userService = userService; // Assign
        this.adsService = adsService;   // Assign
    }

    @Override
    public DashboardStatsResponse getDashboardStats() {
        // ... (unchanged)
        long userCount = userRepository.count();
        long adCount = adsRepository.count();
        long storageCount = storageRepository.count();
        BigDecimal totalRevenue = invoiceRepository.findAll().stream()
                .map(invoice -> invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO) // Null check
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return DashboardStatsResponse.builder()
                .userCount(userCount)
                .adCount(adCount)
                .storageCount(storageCount)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public List<GetUsersResponse> getAllUsers() {
        // Delegate to UserService
        return userService.getAll();
    }

    @Override
    @Transactional // Keep transaction boundary here for the admin action
    public void deleteUser(String userId) {
        logger.warn("Admin attempting to delete user and associated data for ID: {}", userId);
        // Delegate the complex deletion logic (including storage) to UserService
        try {
            deleteUserAndAssociatedData(userId); // Call the renamed method
            logger.info("Admin successfully initiated deletion for user {}", userId);
        } catch (EntityNotFoundException e) {
            logger.error("Admin failed to delete user {}: {}", userId, e.getMessage());
            throw e; // Re-throw to let controller handle response
        } catch (Exception e) {
            logger.error("Admin failed during deletion process for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error during user deletion process", e); // Or a more specific exception
        }
    }
    @Override // Bu metot ExpertService arayüzüne eklenmeli
    @Transactional
    public ResponseEntity<?> deleteExpertAndAssociatedData(String expertId) throws StripeException{
        logger.warn("Attempting to DELETE expert and all associated data for ID: {}", expertId);
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new EntityNotFoundException("Expert not found with ID: " + expertId));

        // Yetki Kontrolü (Admin mi?)

        try {
            // 1. İlişkili Storage'ları sil (Dosya + DB) - UserManager'daki gibi
            List<Storage> expertStorages = new ArrayList<>(expert.getStorages());
            logger.info("Deleting {} associated storage records for expert {}", expertStorages.size(), expertId);
            expert.getStorages().clear();

            for (Storage storage : expertStorages) {
                try {
                    storageManager.deleteImage(storage.getName());
                    storageRepository.delete(storage); // Manuel sil
                } catch (Exception e) { logger.error("Error deleting storage {}", storage.getId(), e); }
            }

            // 2. İlişkili Ads'leri sil (AdsService üzerinden)
            // List<Ads> expertAds = new ArrayList<>(adsRepository.findByUserId(expertId)); // Eğer AdsRepository inject edildiyse
            // logger.info("Deleting {} associated ads for expert {}", expertAds.size(), expertId);
            // for (Ads ad : expertAds) {
            //     try { adsService.delete(ad.getId()); } catch (Exception e) { logger.error(...); }
            // }

            // 3. Stripe Müşterisini Sil
            if (expert.getStripeCustomerId() != null) {
                try { stripeManager.deleteStripeCustomer(expert.getStripeCustomerId()); }
                catch (BusinessException e) { logger.error("Failed to delete Stripe customer for expert {}", expertId, e); }
            }

            // 4. Diğer ilişkili verileri sil (Token, Invoice vb. - UserManager'daki gibi)
            List<Token> tokens = new ArrayList<>(expert.getToken());
            tokenRepository.deleteAll(tokens); // Manuel sil (varsayım)

            // 5. Uzmanı Sil
            logger.info("Deleting expert record for ID: {}", expertId);
            expertRepository.delete(expert);

            logger.info("Successfully deleted expert {} and associated data.", expertId);
            return ResponseEntity.ok("Expert and associated data deleted successfully.");

        } catch (Exception e) {
            logger.error("Failed to complete deletion for expert {}: {}", expertId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete expert and associated data.", e);
        }
    }
    @Override // Bu metot UserService arayüzüne eklenmeli
    @Transactional
    public ResponseEntity<?> deleteUserAndAssociatedData(String id) {
        logger.warn("Attempting to DELETE user and all associated data for ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // **YETKİ KONTROLÜ:** Admin mi veya kullanıcı kendi hesabını mı siliyor?

        try {
            // 1. İlişkili TÜM Storage'ları sil (Profil resmi + varsa diğerleri)
            List<Storage> userStorages = new ArrayList<>(user.getStorages()); // Kopya
            logger.info("Deleting {} associated storage records for user {}", userStorages.size(), id);
            user.getStorages().clear(); // İlişkiyi temizle

            for (Storage storage : userStorages) {
                try {
                    storageManager.deleteImage(storage.getName());
                    storageRepository.delete(storage); // Manuel sil
                } catch (Exception e) { logger.error("Error deleting storage {}", storage.getId(), e); }
            }

            // 2. İlişkili Ads'leri sil (AdsService üzerinden)
            // Bu, Ads->User ilişkisinin nasıl tanımlandığına bağlı.
            // Eğer User silinince Ads'ler otomatik silinmiyorsa (CascadeType.REMOVE yoksa):
            // List<Ads> userAds = new ArrayList<>(adsRepository.findByUserId(id)); // AdsRepository'de findByUserId olmalı
            // logger.info("Deleting {} associated ads for user {}", userAds.size(), id);
            // for (Ads ad : userAds) {
            //     try {
            //         adsService.delete(ad.getId()); // AdsService.delete çağırılmalı
            //     } catch (Exception e) { logger.error("Failed to delete ad {} for user {}: {}", ad.getId(), id, e.getMessage()); }
            // }
            // Eğer User silinince Ads'ler otomatik siliniyorsa (CascadeType.REMOVE veya orphanRemoval=true):
            // Bu adıma gerek yok.

            // 3. İlişkili Token'ları sil
            List<Token> tokens = new ArrayList<>(user.getToken());
            logger.info("Deleting {} associated tokens for user {}", tokens.size(), id);
            user.getToken().clear(); // İlişkiyi temizle (opsiyonel)
            tokenRepository.deleteAll(tokens); // Manuel sil

            // 4. İlişkili Diğer Entity'leri Sil (örn: Invoice, Request - Cascade yoksa)
            // ... (Benzer silme mantığı)

            // 5. Kullanıcıyı Sil
            logger.info("Deleting user record for ID: {}", id);
            userRepository.delete(user);

            logger.info("Successfully deleted user {} and associated data.", id);
            return ResponseEntity.ok("User and associated data deleted successfully.");

        } catch (Exception e) {
            logger.error("Failed to complete deletion for user {}: {}", id, e.getMessage(), e);
            // Transaction rollback olmalı
            throw new RuntimeException("Failed to delete user and associated data.", e); // Controller'a fırlat
        }
    }
    @Override
    public List<GetAllAdsResponse> getAllAds() {
        // Delegate to AdsService
        return adsService.getAll();
    }

    @Override
    @Transactional // Keep transaction boundary here for the admin action
    public void deleteAd(String adId) {
        logger.warn("Admin attempting to delete ad and associated data for ID: {}", adId);
        // Delegate the complex deletion logic (including storage) to AdsService
        try {
            adsService.delete(adId); // AdsService.delete handles storage cleanup
            logger.info("Admin successfully initiated deletion for ad {}", adId);
        } catch (EntityNotFoundException e) {
            logger.error("Admin failed to delete ad {}: {}", adId, e.getMessage());
            throw e; // Re-throw
        } catch (Exception e) {
            logger.error("Admin failed during deletion process for ad {}: {}", adId, e.getMessage(), e);
            throw new RuntimeException("Error during ad deletion process", e);
        }
    }

    @Override
    @Transactional
    public void updateAdStatus(String adId, boolean isActive) {
        logger.info("Admin updating status for ad {} to active={}", adId, isActive);
        var ad = adsRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found with ID: " + adId));
        ad.setActive(isActive);
        adsRepository.save(ad);
        logger.info("Successfully updated status for ad {}", adId);
    }

    @Override
    public List<GetAllImagesResponse> getAllStorages() {
        // Delegate to StorageService/Manager
        return storageManager.getAll();
    }

    @Override
    @Transactional // Keep transaction boundary
    public void deleteStorage(String storageId) {
        logger.warn("Admin attempting to delete storage record and file for ID: {}", storageId);
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new EntityNotFoundException("Storage record not found with ID: " + storageId));

        // Optional: Check if it's a profile image and clear the field on User/Expert
        if (storage.getUser() != null && storage.getName().equals(storage.getUser().getProfileImage())) {
            logger.warn("Deleting storage which is the profile image for user {}", storage.getUser().getId());
            storage.getUser().setProfileImage(null);
            userRepository.save(storage.getUser()); // Save the user change
        }
        // Optional: Check if it's the main image for an Ad and clear/update
        if (storage.getAds() != null && storage.getUrl().equals(storage.getAds().getImageUrl())) {
            Ads ad = storage.getAds();
            // Find a new main image or set to null
            String newImageUrl = ad.getStorages().stream()
                    .filter(s -> !s.getId().equals(storageId)) // Exclude the one being deleted
                    .map(Storage::getUrl)
                    .findFirst()
                    .orElse(null);
            ad.setImageUrl(newImageUrl);
            adsRepository.save(ad);
            logger.warn("Updated main image URL for ad {} because its main image was deleted.", ad.getId());
        }

        // Delete file first
        try {
            storageManager.deleteImage(storage.getName());
        } catch (IOException e) {
            logger.error("Failed to delete storage file {} while deleting record {}: {}", storage.getName(), storageId, e.getMessage());
            // Decide whether to proceed with DB deletion or throw error
            throw new RuntimeException("Failed to delete storage file", e);
        }

        // Delete DB record
        try {
            storageRepository.delete(storage);
            logger.info("Admin successfully deleted storage file {} and record {}", storage.getName(), storageId);
        } catch (Exception e) {
            logger.error("Failed to delete storage record {} after deleting file: {}", storageId, e.getMessage());
            throw new RuntimeException("Failed to delete storage record from database", e);
        }
    }

    // downloadStorage and viewStorage can likely be simplified by delegating to StorageManager.serveImage
    @Override
    public ResponseEntity<?> downloadStorage(String fileName) {
        logger.debug("Admin request to download storage file: {}", fileName);
        // Simply serve the image, the browser will handle download if header is set
        // return storageManager.serveImage(fileName); // Might need Content-Disposition header added

        // Or keep existing logic if specific download behavior needed:
        try {
            Resource resource = storageManager.loadAsResource(fileName);
            if (resource == null || !resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found or readable");
            }
            String contentType = determineContentType(fileName);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"") // Force download
                    .body(resource);
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException for download file {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not determine file path");
        } catch (Exception e) {
            logger.error("Error during download for file {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing download request");
        }
    }

    @Override
    public ResponseEntity<?> viewStorage(String fileName) {
        logger.debug("Admin request to view storage file: {}", fileName);
        // Delegate directly to serveImage which handles content type and serving
        return storageManager.serveImage(fileName);
    }

    // determineContentType - unchanged
    private String determineContentType(String fileName) {
        // ... (same as before)
        if (fileName == null || fileName.lastIndexOf(".") == -1) return "application/octet-stream";
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) { /* ... cases ... */ default: return "application/octet-stream"; }
    }
}
