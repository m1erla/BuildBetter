package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.ExpertService;
import com.renovatipoint.business.requests.UpdateExpertRequest;
import com.renovatipoint.business.responses.GetExpertResponse;
import com.renovatipoint.core.utilities.mappers.ModelMapperService;
import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.dataAccess.abstracts.StorageRepository;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.Expert;
import com.renovatipoint.entities.concretes.Storage;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ExpertManager implements ExpertService {
    private final ExpertRepository expertRepository;

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;

    private final StorageManager storageManager;
    private final StripeManager stripeManager;
    private static final Logger logger = LoggerFactory.getLogger(ExpertManager.class);
    private final ModelMapperService modelMapperService;

    public ExpertManager(ExpertRepository expertRepository, StorageRepository storageRepository,
            UserRepository userRepository, StorageManager storageManager, ModelMapperService modelMapperService,
            StripeManager stripeManager) {
        this.expertRepository = expertRepository;
        this.storageRepository = storageRepository;
        this.userRepository = userRepository;
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
    public ResponseEntity<?> update(UpdateExpertRequest updateExpertRequest) {
        try {
            Expert expert = this.expertRepository.findById(updateExpertRequest.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            // Manuel olarak alanları güncelle
            if (updateExpertRequest.getName() != null)
                expert.setName(updateExpertRequest.getName());
            if (updateExpertRequest.getSurname() != null)
                expert.setSurname(updateExpertRequest.getSurname());
            if (updateExpertRequest.getEmail() != null)
                expert.setEmail(updateExpertRequest.getEmail());
            if (updateExpertRequest.getPhoneNumber() != null)
                expert.setPhoneNumber(updateExpertRequest.getPhoneNumber());
            if (updateExpertRequest.getAddress() != null)
                expert.setAddress(updateExpertRequest.getAddress());
            if (updateExpertRequest.getPostCode() != null)
                expert.setPostCode(updateExpertRequest.getPostCode());
            if (updateExpertRequest.getCompanyName() != null)
                expert.setCompanyName(updateExpertRequest.getCompanyName());
            if (updateExpertRequest.getChamberOfCommerceNumber() != null)
                expert.setChamberOfCommerceNumber(updateExpertRequest.getChamberOfCommerceNumber());

            // Profil resmi güncelleme
            if (updateExpertRequest.getStorages() != null && !updateExpertRequest.getStorages().isEmpty()) {
                try {
                    String oldFileName = expert.getProfileImage();
                    List<Storage> oldStorage = expert.getStorages();
                    if (oldFileName != null && !oldFileName.isEmpty() && oldStorage != null) {
                        storageManager.deleteImage(oldFileName);
                        storageRepository.deleteAll(oldStorage);
                    }
                    String newFileName = storageManager.uploadImage(updateExpertRequest.getStorages(), expert);
                    expert.setProfileImage(newFileName);
                } catch (IOException ex) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to update expert profile image");
                }
            }

            // Uzmanı kaydet
            Expert updatedExpert = expertRepository.save(expert);

            // Güncellenmiş uzman bilgilerini response olarak dön
            GetExpertResponse response = modelMapperService.forResponse()
                    .map(updatedExpert, GetExpertResponse.class);

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Expert not found with ID: " + updateExpertRequest.getId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating expert: " + e.getMessage());
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
