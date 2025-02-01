package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.CategoryService;
import com.renovatipoint.business.requests.CreateCategoryRequest;
import com.renovatipoint.business.requests.UpdateCategoryRequest;
import com.renovatipoint.business.responses.GetAllCategoriesResponse;
import com.renovatipoint.business.responses.GetAllJobTitlesResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@AllArgsConstructor
public class CategoryController {
    private CategoryService categoryService;

    @GetMapping
    public List<GetAllCategoriesResponse> getAllCategories(){
        return categoryService.getAll();
    }

    @GetMapping("/{categoryName}/jobTitles")
    public List<GetAllJobTitlesResponse> getJobTitlesByCategory(@PathVariable String categoryName){
        return categoryService.getJobTitlesByCategory(categoryName);
    }


    @PostMapping("/category")
    @ResponseStatus(code = HttpStatus.CREATED)
    public void add(@RequestBody CreateCategoryRequest createCategoryRequest){
        this.categoryService.add(createCategoryRequest);
    }



    @PutMapping("/category_update/{id}")
    public void update(@RequestBody UpdateCategoryRequest updateCategoryRequest){
        this.categoryService.update(updateCategoryRequest);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id){
        this.categoryService.delete(id);
    }
}
