package com.buildbetter.business.abstracts;

import com.buildbetter.business.requests.CreateCategoryRequest;
import com.buildbetter.business.requests.UpdateCategoryRequest;
import com.buildbetter.business.responses.GetAllCategoriesResponse;
import com.buildbetter.business.responses.GetAllJobTitlesResponse;
import com.buildbetter.business.responses.GetCategoriesByIdResponse;

import java.util.List;

public interface CategoryService {
    List<GetAllCategoriesResponse> getAll();

    GetCategoriesByIdResponse getById(String id);

    void add(CreateCategoryRequest createCategoryRequest);
    void update(UpdateCategoryRequest updateCategoryRequest);

    void delete(String id);

    List<GetAllJobTitlesResponse> getJobTitlesByCategory(String categoryName);
}
