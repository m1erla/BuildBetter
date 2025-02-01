package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.AdsService;
import com.renovatipoint.business.requests.CreateAdsRequest;
import com.renovatipoint.business.requests.UpdateAdsRequest;
import com.renovatipoint.business.responses.GetAllAdsResponse;
import com.renovatipoint.business.responses.GetAllImagesResponse;
import com.renovatipoint.business.rules.AdsBusinessRules;
import com.renovatipoint.core.utilities.mappers.ModelMapperService;
import com.renovatipoint.dataAccess.abstracts.*;
import com.renovatipoint.entities.concretes.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        return ads.stream().map(ad -> this.modelMapperService.forResponse().map(ad, GetAllAdsResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<GetAllAdsResponse> getUserAdById(String userId) {
        List<Ads> ads = this.adsRepository.findByUserId(userId);
        logger.info("Found {} ads for user {}", ads.size(), userId);

        return ads.stream().filter(ad -> ad != null)
                .map(ad -> {
                    GetAllAdsResponse dto = modelMapperService.forResponse().map(ad, GetAllAdsResponse.class);
                    if (dto.getTitle() == null) {
                        logger.warn("Ad with id {} has no title", ad.getId());
                        dto.setTitle("Untitled Ad");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public ResponseEntity<?> getAdImagesForUser(String userId) {
        List<Ads> adsList = adsRepository.findByUserId(userId);

        if (adsList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No ads found for this user");
        }

        List<GetAllImagesResponse> imageResponses = adsList.stream()
                .flatMap(ads -> ads.getStorages().stream())
                .map(image -> {
                    GetAllImagesResponse response = new GetAllImagesResponse();
                    response.setId(image.getId());
                    response.setName(image.getName());
                    response.setType(image.getType());
                    response.setUrl(image.getUrl());
                    return response;
                })
                .collect(Collectors.toList());

        if (imageResponses.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No images found for this user's ads");
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

        if (adsBusinessRules.checkIfAdsNameExists(createAdsRequest.getTitle())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This ad name already exists. Please try a different name to create a new ad!");
        }

        if (createAdsRequest.getDescriptions().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The description field cannot be empty!");
        }

        Category category = categoryRepository.findById(createAdsRequest.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found!"));

        ServiceEntity service = serviceRepository.findById(createAdsRequest.getServiceId())
                .orElseThrow(() -> new EntityNotFoundException("Service not found!"));

        User user = userRepository.findById(createAdsRequest.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found!"));

        // if (user instanceof Expert){
        // Expert expert = (Expert) user;
        // if (expert.getPaymentInfo() == null){
        // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Expert must set up
        // a payment method before posting ads");
        // }
        // }
        Ads ads = this.modelMapperService.forRequest().map(createAdsRequest, Ads.class);

        ads.setUser(user);
        ads.setCategory(category);
        ads.setService(service);

        this.adsRepository.save(ads);

        // Handle image upload
        try {
            if (createAdsRequest.getStorages() != null && !createAdsRequest.getStorages().isEmpty()) {
                List<String> fileNames = storageManager.uploadImages(createAdsRequest.getStorages(), user, ads);
                ads.setImageUrl(fileNames.get(0)); // Assuming you want to set the first image URL
                ads.setStorages(ads.getStorages());
                adsRepository.save(ads);

            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload ad images.");
        }

        return ResponseEntity.ok().body("Ad successfully created!");
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UpdateAdsRequest updateAdsRequest) {
        this.adsBusinessRules.checkIfAdsExists(updateAdsRequest.getId(), updateAdsRequest.getTitle());
        Ads existingAd = this.adsRepository.findById(updateAdsRequest.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ad not found"));

        existingAd.setId(updateAdsRequest.getId());
        existingAd.setTitle(updateAdsRequest.getTitle());
        existingAd.setDescriptions(updateAdsRequest.getDescriptions());
        existingAd.setActive(updateAdsRequest.isActive());
        existingAd.setUpdatedAt(LocalDateTime.now());
        // Update category if provided
        if (updateAdsRequest.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateAdsRequest.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            existingAd.setCategory(category);
        }

        // Update service if provided
        if (updateAdsRequest.getServiceId() != null) {
            ServiceEntity service = serviceRepository.findById(updateAdsRequest.getServiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Service not found"));
            existingAd.setService(service);
        }

        if (updateAdsRequest.getStorages() != null && !updateAdsRequest.getStorages().isEmpty()) {
            try {
                List<Storage> oldStorages = existingAd.getStorages();
                if (oldStorages != null && !oldStorages.isEmpty()) {
                    for (Storage storage : oldStorages) {
                        storageManager.deleteImage(storage.getName());
                        storageRepository.delete(storage);
                    }
                    existingAd.getStorages().clear();
                }

                List<String> fileNames = storageManager.uploadImages(updateAdsRequest.getStorages(),
                        existingAd.getUser(), existingAd);
                existingAd.setImageUrl(fileNames.get(0));
            } catch (IOException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update ad images.");
            }
        }

        this.adsRepository.save(existingAd);

        return ResponseEntity.ok().body("Ad successfully updated!");
    }

    @Transactional
    public List<String> uploadAdImage(String id, List<MultipartFile> files) throws IOException {
        Ads ads = adsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found"));
        List<String> fileNames = storageManager.uploadImages(files, ads.getUser(), ads);
        ads.setImageUrl(fileNames.get(0));
        adsRepository.save(ads);
        return fileNames;
    }

    @Override
    @Transactional
    public ResponseEntity<?> getAdImages(String id) {
        Ads ads = adsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found"));
        List<Storage> storages = ads.getStorages();

        if (storages.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No images found for this ad");
        }

        List<GetAllImagesResponse> imageResponses = storages.stream()
                .map(storage -> new GetAllImagesResponse(storage.getId(), storage.getName(), storage.getType(),
                        storage.getUrl()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(imageResponses);
    }

    @Transactional
    public ResponseEntity<?> deleteAdImage(String id) {
        Storage storage = storageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));
        Ads ads = storage.getAds();
        if (ads != null) {
            ads.getStorages().remove(storage);
            if (ads.getStorages().isEmpty()) {
                ads.setImageUrl(null);
            }
            adsRepository.save(ads);
        }
        storageRepository.deleteByName(storage.getName());
        storageRepository.delete(storage);
        return ResponseEntity.ok("Image deleted successfully");
    }

    @Transactional
    public String updateAdImage(String id, List<MultipartFile> files) throws IOException {
        Ads ads = adsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found"));
        User user = userRepository.findById(ads.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Delete old images if needed
        List<Storage> oldStorages = ads.getStorages();
        if (oldStorages != null && !oldStorages.isEmpty()) {
            for (Storage storage : oldStorages) {
                storageManager.deleteImage(storage.getName());
                storageRepository.delete(storage);
            }
            ads.getStorages().clear();
        }
        List<String> fileNames = storageManager.uploadImages(files, user, ads);
        ads.setImageUrl(fileNames.get(0));
        adsRepository.save(ads);
        return "Ad images updated successfully: " + String.join(",", fileNames);
    }

    public boolean existsById(String id) {
        return adsRepository.existsById(id);
    }

    @Override
    public void delete(String id) {
        this.adsRepository.deleteById(id);
    }
}
