package com.renovatipoint.business.rules;

import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.JobTitleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class JobTitleBusinessRules {
    private JobTitleRepository jobTitleRepository;

    public void checkIfJobTitleNameExists(String jobTitle){
        if (this.jobTitleRepository.existsByName(jobTitle)){
            throw  new BusinessException("The job title is already exist! Please try differ job title name");
        }
    }
}
