package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.AdsService;
import com.buildbetter.business.requests.CreateAdsRequest;
import com.buildbetter.business.requests.UpdateAdsRequest;
import com.buildbetter.business.responses.GetAllAdsResponse;
import com.buildbetter.business.responses.GetAllImagesResponse; // Bu importu ekleyin
import com.buildbetter.business.rules.AdsBusinessRules;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.*;
import com.buildbetter.entities.concretes.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Tarih formatlama için
import java.util.ArrayList;
import java.util.Collections; // Boş liste için
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdsManager implements AdsService {

    private final ModelMapperService modelMapperService;
    private final AdsRepository adsRepository;
    private final AdsBusinessRules adsBusinessRules;

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final StorageManager storageManager;
    private final StorageRepository storageRepository;

    private final static Logger logger = LoggerFactory.getLogger(AdsManager.class);

    // Constructor
    public AdsManager(ModelMapperService modelMapperService, AdsRepository adsRepository,
                      AdsBusinessRules adsBusinessRules, CategoryRepository categoryRepository,
                      ServiceRepository serviceRepository, UserRepository userRepository, StorageManager storageManager,
                      StorageRepository storageRepository) {
        this.modelMapperService = modelMapperService;
        this.adsRepository = adsRepository;
        this.adsBusinessRules = adsBusinessRules;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.storageManager = storageManager;
        this.storageRepository = storageRepository;
    }

    @Override
    public List<GetAllAdsResponse> getAll() {
        List<Ads> ads = this.adsRepository.findAll();
        // İlişkili verilerle birlikte map'leme
        return ads.stream().map(this::mapAdsToGetAllAdsResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<GetAllAdsResponse> getUserAdById(String userId) {
        List<Ads> ads = this.adsRepository.findByUserId(userId);
        logger.info("Found {} ads for user {}", ads.size(), userId);

        return ads.stream()
                .filter(ad -> ad != null)
                .map(this::mapAdsToGetAllAdsResponse) // Ortak map'leme metodu kullan
                .collect(Collectors.toList());
    }

    // --- YENİ getAdById METODU IMPLEMENTASYONU ---
    @Override
    public GetAllAdsResponse getAdById(String adId) {
        Ads ad = this.adsRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("Ad with ID " + adId + " not found"));

        // Ortak map'leme metodunu kullanarak DTO oluştur
        return mapAdsToGetAllAdsResponse(ad);
    }
    // --- ---

    // --- YARDIMCI MAPLEME METODU ---
    // Ads entity'sini GetAllAdsResponse DTO'suna mapler
    private GetAllAdsResponse mapAdsToGetAllAdsResponse(Ads ad) {
        // ... (Önceki adımdaki gibi)
        if (ad == null) return null;
        GetAllAdsResponse dto = modelMapperService.forResponse().map(ad, GetAllAdsResponse.class);
        // Kategori, Servis, Kullanıcı bilgilerini set et (null check ile)
        if (ad.getCategory() != null) dto.setCategoryName(ad.getCategory().getName()); else dto.setCategoryName("N/A");
        if (ad.getService() != null) dto.setServiceName(ad.getService().getName()); else dto.setServiceName("N/A");
        if (ad.getUser() != null) dto.setUserName(ad.getUser().getName() + " " + ad.getUser().getSurname()); else dto.setUserName("N/A");

        // Storages listesini map'le
        if (ad.getStorages() != null && !ad.getStorages().isEmpty()) {
            dto.setStorages(ad.getStorages().stream()
                    .map(storage -> modelMapperService.forResponse().map(storage, GetAllImagesResponse.class))
                    .collect(Collectors.toList()));
            // Ana resmi ayarla
            dto.setImageUrl(ad.getStorages().get(0).getUrl()); // Storage'dan URL al
        } else {
            dto.setStorages(Collections.emptyList());
            dto.setImageUrl(null); // Veya varsayılan URL
        }
        // Tarih formatlama (DTO string ise)
        if (ad.getAdReleaseDate() != null) dto.setAdReleaseDate(ad.getAdReleaseDate().format(DateTimeFormatter.ISO_DATE_TIME));
        if (dto.getTitle() == null || dto.getTitle().isEmpty()) dto.setTitle("Başlıksız İlan");

        return dto;
    }
    // --- ---

    // --- Diğer mevcut metotlar (getAdImagesForUser, add, update, vs.) ---
    // ... (Kodun geri kalanı aynı)
    @Override
    @Transactional
    public ResponseEntity<?> getAdImages(String id) {
        Ads ads = adsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found"));
        List<Storage> storages = ads.getStorages();

        if (storages.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No images found for this ad");
        }

        List<GetAllImagesResponse> imageResponses = storages.stream()
                .map(storage -> modelMapperService.forResponse().map(storage, GetAllImagesResponse.class)) // ModelMapper kullanıldı
                .collect(Collectors.toList());

        return ResponseEntity.ok(imageResponses);
    }

    public ResponseEntity<?> getAdImagesForUser(String userId) {
        List<Ads> adsList = adsRepository.findByUserId(userId);

        if (adsList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No ads found for this user");
        }

        List<GetAllImagesResponse> imageResponses = adsList.stream()
                .flatMap(ads -> ads.getStorages() != null ? ads.getStorages().stream() : Stream.empty()) // Null check eklendi
                .map(image -> modelMapperService.forResponse().map(image, GetAllImagesResponse.class)) // ModelMapper kullanıldı
                .collect(Collectors.toList());

        if (imageResponses.isEmpty()) {
            // Kullanıcının ilanları var ama hiç resmi yoksa boş liste döndür, 404 değil.
            return ResponseEntity.ok(Collections.emptyList());
            // return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No images found for this user's ads");
        }

        return ResponseEntity.ok(imageResponses);
    }


    @Override
    @Transactional
    public ResponseEntity<?> add(CreateAdsRequest createAdsRequest) {
        System.out.println("Received create ad request: " + createAdsRequest); // Logging
        System.out.println("UserId: " + createAdsRequest.getUserId());
        System.out.println("CategoryId: " + createAdsRequest.getCategoryId());
        System.out.println("ServiceId: " + createAdsRequest.getServiceId());
        // ... (kontroller ve entity bulma işlemleri aynı)
        if (adsBusinessRules.checkIfAdsNameExists(createAdsRequest.getTitle())) { /* ... */ }
        if (createAdsRequest.getDescriptions() == null || createAdsRequest.getDescriptions().trim().isEmpty()) { /* ... */ }
        Category category = categoryRepository.findById(createAdsRequest.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found!"));
        ServiceEntity service = serviceRepository.findById(createAdsRequest.getServiceId())
                .orElseThrow(() -> new EntityNotFoundException("Service not found!"));
        User user = userRepository.findById(createAdsRequest.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found!"));

        Ads ads = this.modelMapperService.forRequest().map(createAdsRequest, Ads.class);
        ads.setUser(user);
        ads.setCategory(category);
        ads.setService(service);
        ads.setActive(true);
        ads.setAdReleaseDate(LocalDateTime.now());
        ads.setStorages(new ArrayList<>()); // Başlangıçta boş liste ata

        // Resimleri YÜKLE VE KAYDET (uploadImages artık List<Storage> döndürüyor)
        List<Storage> savedStorages = Collections.emptyList();
        try {
            if (createAdsRequest.getStorages() != null && !createAdsRequest.getStorages().isEmpty()) {
                // uploadImages Storage listesi döndürür ve DB'ye kaydeder
                savedStorages = storageManager.uploadImages(createAdsRequest.getStorages(), user, ads);
                if (!savedStorages.isEmpty()) {
                    ads.setStorages(savedStorages); // Kaydedilen Storage listesini Ads entity'sine ata
                    ads.setImageUrl(savedStorages.get(0).getUrl()); // Ana resmi ayarla (Storage URL'si)
                }
            }
        } catch (IOException e) {
            logger.error("Failed to upload ad images during creation.", e);
            // Transaction rollback olmalı
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload ad images.");
        }

        // İlanı DB'ye kaydet (Resimler StorageManager tarafından zaten kaydedildi)
        Ads savedAd = this.adsRepository.save(ads);

        // Başarılı yanıtı DTO ile döndür
        return ResponseEntity.status(HttpStatus.CREATED).body(mapAdsToGetAllAdsResponse(savedAd));
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UpdateAdsRequest updateAdsRequest) {
        // Mevcut ilanı bul
        this.adsBusinessRules.checkIfAdsExists(updateAdsRequest.getId(), updateAdsRequest.getTitle());
        Ads existingAd = this.adsRepository.findById(updateAdsRequest.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ad not found with ID: " + updateAdsRequest.getId()));

        // Alanları güncelle
        existingAd.setTitle(updateAdsRequest.getTitle());
        existingAd.setDescriptions(updateAdsRequest.getDescriptions());
        existingAd.setActive(updateAdsRequest.isActive());
        existingAd.setUpdatedAt(LocalDateTime.now());

        // Kategori ve Servis güncellemesi (önceki gibi)
        if (updateAdsRequest.getCategoryId() != null && (existingAd.getCategory() == null || !existingAd.getCategory().getId().equals(updateAdsRequest.getCategoryId()))) {
            Category category = categoryRepository.findById(updateAdsRequest.getCategoryId()).orElseThrow(/*...*/);
            existingAd.setCategory(category);
        }
        if (updateAdsRequest.getServiceId() != null && (existingAd.getService() == null || !existingAd.getService().getId().equals(updateAdsRequest.getServiceId()))) {
            ServiceEntity service = serviceRepository.findById(updateAdsRequest.getServiceId()).orElseThrow(/*...*/);
            existingAd.setService(service);
        }

        // Resim Güncellemesi
        if (updateAdsRequest.getStorages() != null && !updateAdsRequest.getStorages().isEmpty()) {
            // Yeni resimler varsa:
            try {
                // 1. Mevcut resim dosyalarını ve DB kayıtlarını sil
                List<Storage> oldStorages = new ArrayList<>(existingAd.getStorages()); // Güvenli silme için kopya oluştur
                if (!oldStorages.isEmpty()) {
                    logger.info("Deleting {} old images for ad {}", oldStorages.size(), existingAd.getId());
                    // İlişkiyi temizle (JPA tarafında)
                    existingAd.getStorages().clear();
                    // ÖNEMLİ: İlişkiyi temizledikten sonra Ads'i kaydetmek,
                    // storageRepository.delete'den önce FK constraint hatası almayı önleyebilir.
                    // Ancak bu, resim yükleme başarısız olursa eski resimlerin ilişkisinin
                    // kaybolmasına neden olabilir. Bu yüzden silmeyi sona bırakmak daha güvenli olabilir.
                    // Şimdilik silmeyi döngüden sonra yapalım.

                    for (Storage oldStorage : oldStorages) {
                        try {
                            storageManager.deleteImage(oldStorage.getName()); // Dosyayı sil
                            // DB KAYDINI MANUEL SİL (Cascade yok!)
                            storageRepository.delete(oldStorage);
                            logger.debug("Deleted old storage record and file: {}", oldStorage.getName());
                        } catch (IOException e) {
                            logger.error("Failed to delete old image file {} for ad {}", oldStorage.getName(), existingAd.getId(), e);
                            // Hata durumunda ne yapılacağına karar verilmeli (örn. transaction rollback)
                        } catch (Exception e) {
                            logger.error("Failed to delete old storage DB record {} for ad {}", oldStorage.getId(), existingAd.getId(), e);
                        }
                    }
                    logger.info("Finished deleting old images and records.");
                    // Eski resim listesini Ads nesnesinden temizleyelim (yukarıda clear() yapıldı)
                    existingAd.setImageUrl(null); // Ana resmi de temizle
                }

                // 2. Yeni resimleri yükle (Bu işlem Storage kaydını da yapar)
                logger.info("Uploading {} new images for ad {}", updateAdsRequest.getStorages().size(), existingAd.getId());
                List<Storage> newStorages = storageManager.uploadImages(updateAdsRequest.getStorages(), existingAd.getUser(), existingAd);

                // 3. Yeni resimleri ilana bağla
                if (!newStorages.isEmpty()) {
                    existingAd.setStorages(newStorages); // Yeni listeyi ata
                    existingAd.setImageUrl(newStorages.get(0).getUrl()); // Yeni ana resmi ayarla
                    logger.info("Successfully uploaded and associated {} new images.", newStorages.size());
                } else {
                    logger.warn("No new images were actually saved for ad {}", existingAd.getId());
                    existingAd.setStorages(Collections.emptyList()); // Boş liste ata
                    existingAd.setImageUrl(null); // Ana resmi null yap
                }

            } catch (IOException ex) {
                logger.error("Failed to update ad images for ad {}", existingAd.getId(), ex);
                // Hata mesajı veya transaction rollback
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update ad images.");
            }
        } else {
            // Yeni resim listesi boş veya null ise: Mevcut resimler silinsin mi?
            // Eğer silinmesi gerekiyorsa, yukarıdaki silme mantığı buraya eklenmeli.
            // Şimdilik dokunmuyoruz (yeni resim yoksa eskiler kalır).
            logger.debug("No new images provided in update request for ad {}", existingAd.getId());
        }

        // Güncellenmiş ilanı kaydet
        Ads updatedAd = this.adsRepository.save(existingAd);
        return ResponseEntity.ok(mapAdsToGetAllAdsResponse(updatedAd));
    }

    @Transactional
    public List<String> uploadAdImage(String id, List<MultipartFile> files) throws IOException {
        Ads ads = adsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found"));
        User user = ads.getUser();
        // StorageManager.uploadImages List<Storage> döndürüyor, List<String> değil.
        List<Storage> savedStorages = storageManager.uploadImages(files, user, ads);

        if (!savedStorages.isEmpty()) {
            // Bu metot sadece ekleme yapıyor, mevcutları silmiyor.
            // Güncelleme senaryosu için update metodu daha uygun.
            if (ads.getStorages() == null) {
                ads.setStorages(new ArrayList<>());
            }
            ads.getStorages().addAll(savedStorages); // Sadece listeye ekle

            if (ads.getImageUrl() == null) { // Ana resim yoksa ilkini ata
                ads.setImageUrl(savedStorages.get(0).getUrl());
            }
            adsRepository.save(ads);
        }
        // Dosya isimlerini döndür
        return savedStorages.stream().map(Storage::getName).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public ResponseEntity<?> deleteAdImage(String imageId) { // imageId ile çalışmalı
        logger.info("Attempting to delete image with ID: {}", imageId);
        Storage storage = storageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with ID: " + imageId));

        Ads ads = storage.getAds();

        // 1. Dosyayı sil
        try {
            storageManager.deleteImage(storage.getName());
            logger.info("Deleted image file: {}", storage.getName());
        } catch (IOException e) {
            logger.error("Failed to delete image file {}: {}", storage.getName(), e.getMessage());
            // Opsiyonel: Hata durumunda DB'den silmeyi durdur
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete image file.");
        }

        // 2. İlan ile olan ilişkiyi kaldır (eğer varsa) ve ilanı güncelle
        if (ads != null) {
            boolean removed = ads.getStorages().removeIf(s -> s.getId().equals(imageId)); // ID ile kaldır
            if (removed) {
                logger.info("Removed image relationship from ad {}", ads.getId());
                // Ana resmi güncelle (gerekirse)
                if (ads.getImageUrl() != null && ads.getImageUrl().equals(storage.getUrl())) {
                    ads.setImageUrl(ads.getStorages().isEmpty() ? null : ads.getStorages().get(0).getUrl());
                    logger.info("Updated main image URL for ad {}", ads.getId());
                }
                adsRepository.save(ads); // İlişki değişikliğini kaydet
            } else {
                logger.warn("Image {} was not found in the storage list of ad {}", imageId, ads.getId());
            }
        } else {
            logger.warn("Image {} was not associated with any ad.", imageId);
        }

        // 3. Storage kaydını DB'den sil (Cascade olmadığı için manuel)
        try {
            storageRepository.delete(storage);
            logger.info("Successfully deleted storage record with ID: {}", imageId);
        } catch (Exception e) {
            logger.error("Failed to delete storage record with ID {}: {}", imageId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete image record from database.");
        }


        return ResponseEntity.ok("Image deleted successfully");
    }


    @Transactional // String ID ile update metodu, kullanılmıyorsa kaldırılabilir.
    public String updateAdImage(String id, List<MultipartFile> files) throws IOException {
        // Bu metot update(UpdateAdsRequest) ile çakışıyor gibi. Muhtemelen kullanılmıyor.
        // Eğer kullanılıyorsa, update metodu ile birleştirilmeli veya mantığı netleştirilmeli.
        logger.warn("Deprecated updateAdImage(String id, List<MultipartFile> files) called for ad {}", id);
        // Geçici olarak hata döndürelim veya update metodunu çağıralım.
        throw new UnsupportedOperationException("This method might be deprecated. Use update(UpdateAdsRequest) instead.");
    }

    public boolean existsById(String id) {
        return adsRepository.existsById(id);
    }

    @Override
    @Transactional
    public void delete(String id) {
        logger.info("Attempting to delete ad with ID: {}", id);
        Ads ad = adsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cannot delete. Ad not found with ID: " + id));

        // İlişkili resimleri sil (Dosya + DB Kaydı)
        List<Storage> storagesToDelete = new ArrayList<>(ad.getStorages()); // Kopya al
        logger.info("Found {} associated images to delete for ad {}", storagesToDelete.size(), id);

        // Önce ilişkileri temizle (opsiyonel ama bazen FK sorunlarını önler)
        ad.setStorages(new ArrayList<>());
        adsRepository.save(ad); // Değişikliği kaydet

        for (Storage storage : storagesToDelete) {
            try {
                storageManager.deleteImage(storage.getName()); // Dosyayı sil
                // Cascade yok, DB'den MANUEL SİL
                storageRepository.delete(storage);
                logger.info("Deleted associated image file {} and record {} for ad {}", storage.getName(), storage.getId(), id);
            } catch (IOException e) {
                logger.error("Failed to delete image file {} while deleting ad {}", storage.getName(), id, e);
                // Hata olsa bile devam etmeyi seçebiliriz veya transaction rollback olmalı
            } catch(Exception e) {
                logger.error("Failed to delete storage record {} while deleting ad {}", storage.getId(), id, e);
                // Hata olsa bile devam etmeyi seçebiliriz veya transaction rollback olmalı
            }
        }

        // Son olarak ilanı sil
        this.adsRepository.delete(ad);
        logger.info("Successfully deleted ad with ID: {}", id);
    }
    // AdsManager sınıfının sonu
}