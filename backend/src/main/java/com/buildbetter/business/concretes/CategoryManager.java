package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.CategoryService;
import com.buildbetter.business.requests.CreateCategoryRequest;
import com.buildbetter.business.requests.UpdateCategoryRequest;
import com.buildbetter.business.responses.GetAllCategoriesResponse;
import com.buildbetter.business.responses.GetAllJobTitlesResponse;
import com.buildbetter.business.responses.GetCategoriesByIdResponse;
import com.buildbetter.business.rules.CategoryBusinessRules;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.CategoryRepository;
import com.buildbetter.entities.concretes.Category;
import com.buildbetter.entities.concretes.JobTitle;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Lazy
public class CategoryManager implements CategoryService {

    private ModelMapperService modelMapperService;
    private CategoryRepository categoryRepository;
    private CategoryBusinessRules categoryBusinessRules;
    @Override
    public List<GetAllCategoriesResponse> getAll() {
        List<Category> categories = categoryRepository.findAll();

        List<GetAllCategoriesResponse> categoriesResponses =
                categories.stream().map(category ->
                        this.modelMapperService.forResponse().map(category, GetAllCategoriesResponse.class)).collect(Collectors.toList());

        return categoriesResponses;

    }

    @Override
    public GetCategoriesByIdResponse getById(String id) {
        Category category = this.categoryRepository.findById(id)
                .orElseThrow(EntityNotFoundException::new);


              return this.modelMapperService.forResponse().map(category, GetCategoriesByIdResponse.class);

    }


    @Override
    public void add(CreateCategoryRequest createCategoryRequest) {
        this.categoryBusinessRules.checkIfCategoryExists(createCategoryRequest.getName());

        Category category = this.modelMapperService.forRequest().map(createCategoryRequest, Category.class);
        this.categoryRepository.save(category);
    }

    @Override
    public void update(UpdateCategoryRequest updateCategoryRequest) {
        Category category = this.modelMapperService.forRequest().map(updateCategoryRequest, Category.class);
        this.categoryRepository.save(category);

    }

    @Override
    public void delete(String id) {
        this.categoryRepository.deleteById(id);

    }

    @Override
    public List<GetAllJobTitlesResponse> getJobTitlesByCategory(String categoryName) {
        Category category = categoryRepository.findByName(categoryName).orElseThrow(() -> new EntityNotFoundException("Category not found!"));
        List<JobTitle> jobTitles = category.getJobTitles();

        return jobTitles.stream()
                .map(jobTitle -> modelMapperService.forResponse().map(jobTitle, GetAllJobTitlesResponse.class))
                .collect(Collectors.toList());


    }
}
