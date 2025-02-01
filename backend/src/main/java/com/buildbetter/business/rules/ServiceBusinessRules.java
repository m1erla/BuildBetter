package com.renovatipoint.business.rules;

import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.ServiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ServiceBusinessRules {
    private ServiceRepository serviceRepository;

    public void checkIfServiceExists(String serviceName){
        if (serviceRepository.existsByName(serviceName)){
            throw new BusinessException("This service name is already exist! Please try different name");
        }
    }
}
