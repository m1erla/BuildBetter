package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.AdminService;
import com.buildbetter.business.responses.GetAllAdsResponse;
import com.buildbetter.business.responses.GetAllImagesResponse;
import com.buildbetter.business.responses.GetUsersResponse;
import com.buildbetter.business.responses.admin.DashboardStatsResponse;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.AdsRepository;
import com.buildbetter.dataAccess.abstracts.InvoiceRepository;
import com.buildbetter.dataAccess.abstracts.StorageRepository;
import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminManager implements AdminService {

    private static final String UPLOAD_DIR = "backend/src/main/resources/static/uploads";

    private final UserRepository userRepository;
    private final AdsRepository adsRepository;
    private final StorageRepository storageRepository;
    private final InvoiceRepository invoiceRepository;
    private final ModelMapperService modelMapperService;
    private final StorageManager storageManager;

    @Autowired
    public AdminManager(
            UserRepository userRepository,
            AdsRepository adsRepository,
            StorageRepository storageRepository,
            InvoiceRepository invoiceRepository,
            ModelMapperService modelMapperService,
            StorageManager storageManager) {
        this.userRepository = userRepository;
        this.adsRepository = adsRepository;
        this.storageRepository = storageRepository;
        this.invoiceRepository = invoiceRepository;
        this.modelMapperService = modelMapperService;
        this.storageManager = storageManager;
    }

    @Override
    public DashboardStatsResponse getDashboardStats() {
        long userCount = userRepository.count();
        long adCount = adsRepository.count();
        long storageCount = storageRepository.count();

        // Tüm faturaların toplamını hesapla
        BigDecimal totalRevenue = invoiceRepository.findAll().stream()
                .map(invoice -> invoice.getAmount())
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
        var users = userRepository.findAll();
        return users.stream()
                .map(user -> modelMapperService.forResponse().map(user, GetUsersResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Kullanıcıya ait depolanmış dosyaları silmek için
        if (user.getProfileImage() != null) {
            try {
                storageManager.deleteImage(user.getProfileImage());
            } catch (IOException e) {
                throw new RuntimeException("Profil resmi silinirken hata oluştu", e);
            }
        }

        // Kullanıcıyı sil
        userRepository.delete(user);
    }

    @Override
    public List<GetAllAdsResponse> getAllAds() {
        var ads = adsRepository.findAll();
        return ads.stream()
                .map(ad -> modelMapperService.forResponse().map(ad, GetAllAdsResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAd(String adId) {
        var ad = adsRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("İlan bulunamadı"));

        // İlanı sil
        adsRepository.delete(ad);
    }

    @Override
    @Transactional
    public void updateAdStatus(String adId, boolean isActive) {
        var ad = adsRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("İlan bulunamadı"));

        ad.setActive(isActive);
        adsRepository.save(ad);
    }

    @Override
    public List<GetAllImagesResponse> getAllStorages() {
        var storages = storageRepository.findAll();
        return storages.stream()
                .map(storage -> modelMapperService.forResponse().map(storage, GetAllImagesResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStorage(String storageId) {
        var storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));

        try {
            storageManager.deleteImage(storage.getName());
        } catch (IOException e) {
            throw new RuntimeException("Dosya silinirken hata oluştu", e);
        }

        // Depolama kaydını sil
        storageRepository.delete(storage);
    }

    @Override
    public ResponseEntity<?> downloadStorage(String fileName) {
        try {
            Optional<Storage> dbImageData = storageRepository.findByName(fileName);
            if (dbImageData.isPresent()) {
                // Dosya yolunu oluştur
                Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
                Resource resource = new UrlResource(filePath.toUri());

                if (resource.exists() || resource.isReadable()) {
                    String contentType = determineContentType(fileName);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dosya bulunamadı");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dosya bilgisi bulunamadı");
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Dosya yüklenirken hata oluştu");
        }
    }

    @Override
    public ResponseEntity<?> viewStorage(String fileName) {
        try {
            Optional<Storage> dbImageData = storageRepository.findByName(fileName);
            if (dbImageData.isPresent()) {
                byte[] imageData = storageManager.downloadImage(fileName);
                return ResponseEntity.status(HttpStatus.OK)
                        .contentType(MediaType.valueOf(dbImageData.get().getType()))
                        .body(imageData);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dosya bulunamadı");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Dosya yüklenirken hata oluştu");
        }
    }

    private String determineContentType(String fileName) {
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Paths.get(fileName));
            if (contentType == null) {
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                }
            }
        } catch (IOException e) {
            // Hata durumunda varsayılan olarak octet-stream kullan
        }
        return contentType;
    }
}