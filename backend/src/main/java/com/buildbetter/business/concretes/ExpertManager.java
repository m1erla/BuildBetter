package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.ExpertService;
import com.buildbetter.business.requests.UpdateExpertRequest;
import com.buildbetter.business.responses.GetExpertResponse;
import com.buildbetter.core.utilities.exceptions.BusinessException;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.ExpertRepository;
import com.buildbetter.dataAccess.abstracts.JobTitleRepository;
import com.buildbetter.dataAccess.abstracts.StorageRepository;
import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.Expert;
import com.buildbetter.entities.concretes.Storage;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class ExpertManager implements ExpertService {
    private final ExpertRepository expertRepository;

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final JobTitleRepository jobTitleRepository;

    private final StorageManager storageManager;
    private final StripeManager stripeManager;
    private static final Logger logger = LoggerFactory.getLogger(ExpertManager.class);
    private final ModelMapperService modelMapperService;

    public ExpertManager(ExpertRepository expertRepository, StorageRepository storageRepository,
                         UserRepository userRepository, JobTitleRepository jobTitleRepository, StorageManager storageManager, ModelMapperService modelMapperService,
                         StripeManager stripeManager) {
        this.expertRepository = expertRepository;
        this.storageRepository = storageRepository;
        this.userRepository = userRepository;
        this.jobTitleRepository = jobTitleRepository;
        this.storageManager = storageManager;
        this.modelMapperService = modelMapperService;
        this.stripeManager = stripeManager;

    }

    @Override
    public GetExpertResponse getExpertById(String expertId) {
        Expert expert = this.expertRepository.findById(expertId).orElseThrow();

        return this.modelMapperService.forResponse().map(expert, GetExpertResponse.class);
    }

    @Override
    public Expert getById(String expertId) {
        Expert expert = this.expertRepository.findById(expertId)
                .orElseThrow(() -> new EntityNotFoundException("Expert not found"));
        return this.modelMapperService.forResponse().map(expert, Expert.class);
    }

    @Override
    public Expert getByEmail(String email) {
        Expert expert = this.expertRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Expert not fount with this email"));
        return this.modelMapperService.forResponse().map(expert, Expert.class);
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UpdateExpertRequest updateExpertRequest) {
        try {
            Expert expert = this.expertRepository.findById(updateExpertRequest.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expert not found with ID: " + updateExpertRequest.getId()));

            // --- YETKİ KONTROLÜ BAŞLANGICI ---
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("Attempt to update expert {} without authentication.", updateExpertRequest.getId());
                // Use standard Spring Security exception handling via SecurityConfig if
                // possible,
                // otherwise return FORBIDDEN or UNAUTHORIZED
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
                // throw new AccessDeniedException("User not authenticated."); // Alternative
            }

            String currentPrincipalName = authentication.getName(); // Kullanıcının email'i

            // Admin kontrolü (Rol adını 'ROLE_ADMIN' olarak varsayıyoruz, SecurityConfig'e
            // göre ayarlanmalı)
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            // Sahip kontrolü (Email ile)
            boolean isOwner = expert.getEmail().equalsIgnoreCase(currentPrincipalName); // Büyük/küçük harf duyarsız
                                                                                        // karşılaştırma

            // Eğer kullanıcı admin değilse VE profilin sahibi değilse, erişimi engelle
            if (!isAdmin && !isOwner) {
                logger.warn("Access denied for user {} attempting to update expert {} ({})", currentPrincipalName,
                        expert.getId(), expert.getEmail());
                // throw new AccessDeniedException("You do not have permission to update this
                // expert profile.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to update this expert profile.");
            }
            logger.info("User {} authorized to update expert {}", currentPrincipalName, expert.getId());
            // --- YETKİ KONTROLÜ SONU ---

            // Mevcut güncelleme kodları...
            if (updateExpertRequest.getName() != null)
                expert.setName(updateExpertRequest.getName());
            if (updateExpertRequest.getSurname() != null)
                expert.setSurname(updateExpertRequest.getSurname());
            // email güncellemesi dikkatli yapılmalı, genellikle ayrı bir işlemle yapılır.
            if (updateExpertRequest.getCompanyName() != null)
                expert.setCompanyName(updateExpertRequest.getCompanyName());
            if (updateExpertRequest.getChamberOfCommerceNumber() != null)
                expert.setChamberOfCommerceNumber(updateExpertRequest.getChamberOfCommerceNumber());
            if (updateExpertRequest.getAddress() != null)
                expert.setAddress(updateExpertRequest.getAddress());
            // jobTitleId ve jobTitleName için mantık (belki ayrı bir servis?)
            if (updateExpertRequest.getJobTitleId() != null) {
                // JobTitle'ı bul ve ata, yoksa hata ver
                expert.setJobTitle(jobTitleRepository.findById(updateExpertRequest.getJobTitleId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Job title not found with ID: " + updateExpertRequest.getJobTitleId())));
            }
            if (updateExpertRequest.getPhoneNumber() != null)
                expert.setPhoneNumber(updateExpertRequest.getPhoneNumber());
            if (updateExpertRequest.getPostCode() != null)
                expert.setPostCode(updateExpertRequest.getPostCode());
            // paymentInfo için mantık (muhtemelen ayrı bir endpoint/servis ile yönetilmeli)
            if (updateExpertRequest.getPaymentInfo() != null) {
                // Ödeme bilgisi güncelleme mantığı buraya gelebilir veya ayrı bir servis
                // çağrılabilir.
                // Örnek:
                // expert.setPaymentInfo(paymentInfoService.update(updateExpertRequest.getPaymentInfo()));
                logger.warn("PaymentInfo update via Expert update is not fully implemented yet.");
                // Şimdilik sadece loglayalım, tam implementasyon gerektirir.
                // expert.setPaymentInfo(updateExpertRequest.getPaymentInfo()); // Direkt atama
                // riskli olabilir.
            }

            // Profil Resmi Güncelleme
            MultipartFile newProfileImageFile = updateExpertRequest.getProfileImageFile();
            if (newProfileImageFile != null && !newProfileImageFile.isEmpty()) {
                logger.info("Updating profile image for expert {}", expert.getId());
                String oldFileName = expert.getProfileImage(); // User'dan profil resmi adını al

                // 1. Eski resmi sil (Dosya + DB Kaydı)
                if (oldFileName != null && !oldFileName.isBlank()) {
                    // Eski Storage kaydını bul (userId yerine expertId kullanılıyor, bu
                    // User->Expert ilişkisine bağlı)
                    // User entity'sinde storages ilişkisi nasıl tanımlı? expertId mi user'ın ID'si
                    // mi?
                    // Şimdilik expert.getId() kullanıyoruz.
                    Optional<Storage> oldStorageOpt = storageRepository.findByNameAndUserId(oldFileName,
                            expert.getId());
                    if (oldStorageOpt.isPresent()) {
                        Storage oldStorage = oldStorageOpt.get();
                        try {
                            storageManager.deleteImage(oldFileName); // Sadece isimle silme metodu varsayılıyor
                            logger.info("Deleted old image file: {}", oldFileName);
                        } catch (IOException e) {
                            logger.error("Error deleting old image file {}: {}", oldFileName, e.getMessage());
                            // Hata olsa bile devam et, DB kaydını silmeye çalış
                        }
                        try {
                            // İlişkiyi User tarafından yönetmek daha iyi olabilir (orphanRemoval=true ile)
                            // Ama manuel silme yapıyorsak:
                            // expert.getStorages().remove(oldStorage); // Önce listeden çıkar
                            storageRepository.delete(oldStorage); // Sonra DB'den sil
                            logger.info("Deleted old storage record for expert {} with name {}", expert.getId(),
                                    oldFileName);
                        } catch (Exception e) {
                            logger.error("Error deleting old storage record for expert {}: {}", expert.getId(),
                                    oldFileName, e);
                            // Bu kritik bir hata olabilir, işlemi durdurabiliriz
                            throw new BusinessException("Failed to remove old profile image record.");
                        }
                    } else {
                        logger.warn(
                                "Old storage record not found for expert {} with name {}. Attempting to delete file only.",
                                expert.getId(), oldFileName);
                        try {
                            storageManager.deleteImage(oldFileName);
                            logger.info("Deleted orphan image file: {}", oldFileName);
                        } catch (IOException e) {
                            logger.error("Error deleting orphan image file {}: {}", oldFileName, e.getMessage());
                        }
                    }
                    expert.setProfileImage(null); // User'daki alanı temizle
                }

                // 2. Yeni resmi yükle (Dosya + DB Kaydı)
                try {
                    // uploadImage metodunun User nesnesi aldığını varsayıyoruz (Storage'daki ilişki
                    // için)
                    // Expert, User'dan türediği için doğrudan gönderebiliriz.
                    Storage newStorage = storageManager.uploadImage(newProfileImageFile, expert);
                    expert.setProfileImage(newStorage.getName()); // Yeni dosya adını ata
                    // Eğer User->Storage ilişkisi CascadeType.PERSIST/ALL içeriyorsa ve
                    // expert.getStorages().add(newStorage) yapıyorsak,
                    // storageRepository.save(newStorage) gerekmeyebilir.
                    // StorageManager'da save yapılıyorsa burası OK.
                    // if (expert.getStorages() == null) expert.setStorages(new ArrayList<>()); //
                    // İlişki varsa gereksiz
                    // expert.getStorages().add(newStorage); // Eğer CascadeType.ALL yoksa veya
                    // ilişkiyi manuel yönetiyorsak
                    logger.info("Successfully updated profile image for expert {} to {}", expert.getId(),
                            newStorage.getName());
                } catch (IOException ex) {
                    logger.error("Failed to upload new profile image for expert {}: {}", expert.getId(),
                            ex.getMessage());
                    throw new BusinessException("Failed to update profile image file.");
                }
            }

            Expert updatedExpert = expertRepository.save(expert);
            GetExpertResponse response = modelMapperService.forResponse().map(updatedExpert, GetExpertResponse.class);
            logger.info("Expert profile updated successfully for ID: {}", updatedExpert.getId());
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            logger.warn("Expert update failed - Not Found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) { // Bu catch bloğu eklendi
            logger.warn("Expert update failed - Access Denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (BusinessException e) {
            logger.warn("Expert update failed - Business Rule Violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Beklenmedik hataları logla ama kullanıcıya genel bir mesaj ver
            logger.error("An unexpected error occurred during expert update for ID {}: {}",
                    updateExpertRequest != null ? updateExpertRequest.getId() : "unknown", // ID null olabilir mi?
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while updating the expert profile.");
        }
    }

    @Override
    public Expert save(Expert expert) {
        try {
            // Stripe müşteri ID'si oluştur
            String stripeCustomerId = stripeManager.createStripeCustomer(expert.getEmail(), expert.getName());
            expert.setStripeCustomerId(stripeCustomerId);
            return expertRepository.save(expert);
        } catch (StripeException e) {
            logger.error("Error creating Stripe customer for expert: {}", expert.getEmail(), e);
            throw new RuntimeException("Failed to create Stripe customer", e);
        }
    }

}
