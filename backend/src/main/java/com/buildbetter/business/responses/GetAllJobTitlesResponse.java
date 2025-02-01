package com.renovatipoint.business.responses;

import com.renovatipoint.entities.concretes.JobTitle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllJobTitlesResponse {
    private String id;
    private String name;
    private String descriptions;
    private String categoryName;
    private String categoryId;


    public GetAllJobTitlesResponse(JobTitle entity, List<GetAllCategoriesResponse> categories){
        this.id = entity.getId();
        this.name = entity.getName();
        this.descriptions = entity.getDescriptions();
        this.categoryName = entity.getCategory().getName();
        this.categoryId = entity.getCategory().getId();
    }
}
