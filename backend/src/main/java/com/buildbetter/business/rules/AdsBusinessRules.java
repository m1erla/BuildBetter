package com.renovatipoint.business.rules;

import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.AdsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdsBusinessRules {
    private AdsRepository adsRepository;

    public boolean checkIfAdsNameExists(String adTitle) {
        boolean exists = adsRepository.existsByTitle(adTitle);
        System.out.println("Checking if ad name exists: " + adTitle + " - Exists: " + exists); // Logging
        return exists;
    }

    public void checkIfAdsActive(boolean isActive){
        if (adsRepository.isActive(isActive)){
            throw new BusinessException("There is no ad here!");
        }
    }

    public void checkIfAdsExists(String id, String adTitle){
        if (adsRepository.existsById(id) && adsRepository.existsByTitle(adTitle)){
            throw new BusinessException("This ad is already exists! Please try to create another ad.");
        }
    }

}
