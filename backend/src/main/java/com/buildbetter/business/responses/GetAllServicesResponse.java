package com.buildbetter.business.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllServicesResponse {
    private String id;
    private String name;
    private String jobTitleName;
    private String categoryId;
    private String categoryName;
    private boolean isActive;

}
