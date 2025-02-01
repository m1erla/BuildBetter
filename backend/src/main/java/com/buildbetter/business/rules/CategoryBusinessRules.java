package com.renovatipoint.business.rules;

import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CategoryBusinessRules {

    private CategoryRepository categoryRepository;

    public void checkIfCategoryExists(String category){
        if (categoryRepository.existsByName(category)){
            throw new BusinessException("This category name is already exists! Please try different name");
        }
    }
}
