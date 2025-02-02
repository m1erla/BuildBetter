package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.CategoryService;
import com.buildbetter.business.requests.CreateCategoryRequest;
import com.buildbetter.business.requests.UpdateCategoryRequest;
import com.buildbetter.business.responses.GetAllCategoriesResponse;
import com.buildbetter.business.responses.GetAllJobTitlesResponse;
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
