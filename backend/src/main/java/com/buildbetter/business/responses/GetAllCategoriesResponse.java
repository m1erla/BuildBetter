package com.renovatipoint.business.responses;

import com.renovatipoint.entities.concretes.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllCategoriesResponse {
    private String id;
    private String name;
    private boolean isActive;


    public GetAllCategoriesResponse(Category entity){
        this.id = entity.getId();
        this.name = entity.getName();
        this.isActive = entity.isActive();
    }



}
