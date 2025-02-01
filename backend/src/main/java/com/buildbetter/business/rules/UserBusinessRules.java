package com.renovatipoint.business.rules;

import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserBusinessRules {

    private UserRepository userRepository;

    public void checkIfEmailExists(String email){
        if (userRepository.existsByEmail(email)){
            throw new BusinessException("This email is already exists!");
        }
    }

    public boolean userExists(String email){
        return userRepository.findByEmail(email).isPresent();
    }
    public boolean userExistsByPhoneNumber(String phoneNumber){
        return userRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

}
