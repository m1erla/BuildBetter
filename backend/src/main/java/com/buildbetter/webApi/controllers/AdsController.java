package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.AdsService;
import com.buildbetter.business.concretes.StorageManager;
import com.buildbetter.business.requests.CreateAdsRequest;
import com.buildbetter.business.requests.UpdateAdsRequest;
import com.buildbetter.business.responses.GetAllAdsResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ads")
@AllArgsConstructor
public class AdsController {

    private static final Logger logger = LoggerFactory.getLogger(AdsController.class);

    private final AdsService adsService;
    private final StorageManager storageManager; // Sadece resim sunmak için

    // --- GET Endpoints ---

    @GetMapping
    public ResponseEntity<List<GetAllAdsResponse>> getAllAds() {
        logger.info("Request received to get all ads");
        List<GetAllAdsResponse> ads = adsService.getAll();
        return ResponseEntity.ok(ads);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GetAllAdsResponse>> getUserAds(@PathVariable String userId) {
        // Yetkilendirme (Authorization) kontrolü servis katmanında yapılmalı
        logger.info("Request received to get ads for user: {}", userId);
        // Servis katmanı doğrudan List döndürdüğü için try-catch burada gereksiz olabilir
        // Eğer servis Exception fırlatırsa, GlobalExceptionHandler yakalamalı.
        List<GetAllAdsResponse> userAds = adsService.getUserAdById(userId);
        return ResponseEntity.ok(userAds);
        // catch (Exception e) { ... } // Global handler varsa gereksiz
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAdById(@PathVariable String id) {
        logger.info("Request received to get ad by ID: {}", id);
        try {
            GetAllAdsResponse adResponse = adsService.getAdById(id);
            return ResponseEntity.ok(adResponse);
        } catch (EntityNotFoundException e) {
            logger.warn("Ad not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving ad details for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving ad details");
        }
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<?> getAdImages(@PathVariable String id) {
        logger.info("Request received to get images for ad ID: {}", id);
        // Servis katmanı ResponseEntity döndürüyor
        return adsService.getAdImages(id);
    }

    // Resim sunma endpoint'i
    @GetMapping("/{id}/images/{imageName}")
    public ResponseEntity<?> serveImage(@PathVariable String id, @PathVariable String imageName) {
        // Bu endpoint doğrudan StorageManager/Service'e delege edilebilir
        // veya güvenlik amacıyla önce ilan var mı diye bakılabilir.
        logger.debug("Request received to serve image {} for ad ID: {}", imageName, id);
        // if (!adsService.existsById(id)) { return ResponseEntity.notFound().build(); } // Opsiyonel kontrol
        return storageManager.serveImage(imageName); // StorageManager halleder
    }

    // --- POST / PUT / DELETE Endpoints ---

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addAd(@ModelAttribute CreateAdsRequest createAdsRequest) {
        // Güvenlik Notu: Servis katmanı createAdsRequest.getUserId()'nin
        // isteği yapan kullanıcıyla eşleştiğini doğrulamalıdır.
        logger.info("Request received to add a new ad");
        return adsService.add(createAdsRequest); // Servis ResponseEntity döndürmeli
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // tüketime MULTIPART_FORM_DATA_VALUE eklendi
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateAd(@PathVariable String id, @ModelAttribute UpdateAdsRequest updateAdsRequest) {
        logger.info("Request received to update ad with ID: {}", id);
        updateAdsRequest.setId(id); // Path variable'ı DTO'ya ata
        // Güvenlik Notu: Servis katmanı, isteği yapan kullanıcının
        // bu ilanın sahibi olduğunu (updateAdsRequest.getUserId() ile) doğrulamalıdır.
        return adsService.update(updateAdsRequest); // Servis ResponseEntity döndürmeli
    }

    // Belirli bir resmi silmek için endpoint
    @DeleteMapping("/images/{imageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAdImage(@PathVariable String imageId) {
        logger.info("Request received to delete image with ID: {}", imageId);
        // Güvenlik Notu: Servis katmanı, isteği yapan kullanıcının
        // bu resmi (imageId'ye ait ilanın sahibi olarak) silme yetkisi olduğunu doğrulamalıdır.
        return adsService.deleteAdImage(imageId); // Servis ResponseEntity döndürmeli
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAd(@PathVariable String id) {
        logger.info("Request received to delete ad with ID: {}", id);
        try {
            // Güvenlik Notu: Servis katmanı, isteği yapan kullanıcının
            // bu ilanı silme yetkisi olduğunu doğrulamalıdır.
            adsService.delete(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            logger.warn("Attempted to delete non-existent ad ID: {}", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (Exception e) {
            logger.error("Error deleting ad ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Yardımcı Metot (determineContentType - StorageManager'da da olabilir) ---
    private String determineContentType(String filename) {
        // Önceki gibi, dosya uzantısına göre content type belirle
        if (filename == null || filename.lastIndexOf(".") == -1) return "application/octet-stream";
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) { /* ... cases ... */ default: return "application/octet-stream"; }
    }

}
