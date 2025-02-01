package com.renovatipoint.business.abstracts;

import com.renovatipoint.business.requests.CreateCategoryRequest;
import com.renovatipoint.business.requests.UpdateCategoryRequest;
import com.renovatipoint.business.responses.GetAllCategoriesResponse;
import com.renovatipoint.business.responses.GetAllJobTitlesResponse;
import com.renovatipoint.business.responses.GetCategoriesByIdResponse;

import java.util.List;

public interface CategoryService {
    List<GetAllCategoriesResponse> getAll();

    GetCategoriesByIdResponse getById(String id);

    void add(CreateCategoryRequest createCategoryRequest);
    void update(UpdateCategoryRequest updateCategoryRequest);

    void delete(String id);

    List<GetAllJobTitlesResponse> getJobTitlesByCategory(String categoryName);
}
